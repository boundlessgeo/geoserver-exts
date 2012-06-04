package org.opengeo.data.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportFilter;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * REST resource for /contexts[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ImportResource extends AbstractResource {

    Importer importer;
    Object importContext; // ImportContext or Iterator<ImportContext>

    public ImportResource(Importer importer) {
        this.importer = importer;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Collections.singletonList(new ImportContextJSONFormat());
    }

    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new ImportContextJSONFormat();
        }
        getResponse().setEntity(formatGet.toRepresentation(lookupContext(true, false)));
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public boolean allowDelete() {
        boolean allowDelete = false;
        importContext = lookupContext(true, false);
        if (importContext instanceof ImportContext) {
            ImportContext ctx = (ImportContext) importContext;
            allowDelete = ctx.getState() != ImportContext.State.COMPLETE;
        } else {
            allowDelete = true;
        }
        return allowDelete;
    }

    @Override
    public void handleDelete() {
        Iterator<ImportContext> contexts = null;
        if (importContext instanceof ImportContext) {
            contexts = Collections.singletonList((ImportContext) importContext).iterator();
        } else {
            contexts = (Iterator<ImportContext>) importContext;
        }
        while (contexts.hasNext()) {
            ImportContext ctx = contexts.next();
            if (ctx.getState() != ImportContext.State.COMPLETE) {
                try {
                    importer.delete(ctx);
                } catch (IOException ioe) {
                    throw new RestletException("Error deleting context " + ctx.getId(), Status.SERVER_ERROR_INTERNAL, ioe);
                }
            }
        }
        getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    }
    
    @Override
    public void handlePost() {
        Object obj = lookupContext(true, true);
        ImportContext context = null;
        if (obj instanceof ImportContext) {
            //run an existing import
            try {
                Form query = getRequest().getResourceRef().getQueryAsForm();
                context = (ImportContext) obj;
                if (query.getNames().contains("async")) {
                    importer.runAsync(context, ImportFilter.ALL);
                } else {
                    importer.run(context);
                    // @todo revisit - if errors occur, they are logged. A second request
                    // is required to verify success
                }
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            } catch (Exception e) {
                throw new RestletException("Error occured executing import", Status.SERVER_ERROR_INTERNAL, e);
            }
        }
        else {
            //create a new import
            try {
                context = importer.createContext(null);
                context.setUser(getCurrentUser());

                if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType())) {
                    //read representation specified by user, use it to read 
                    ImportContext newContext = 
                        (ImportContext) getFormatPostOrPut().toObject(getRequest().getEntity());
                    if (newContext.getTargetWorkspace() != null) {
                        context.setTargetWorkspace(newContext.getTargetWorkspace());
                    }
                    if (newContext.getTargetStore() != null) {
                        context.setTargetStore(newContext.getTargetStore());
                    }
                }

                getResponse().redirectSeeOther(getPageInfo().rootURI("/imports/"+context.getId()));
                getResponse().setEntity(new ImportContextJSONFormat().toRepresentation(context));
                getResponse().setStatus(Status.SUCCESS_CREATED);
            } 
            catch (Exception e) {
                throw new RestletException("Unable to create import", Status.SERVER_ERROR_INTERNAL, e);
            }
        }
        if (context != null) {
            importer.changed(context);
        }
    }

    private String getCurrentUser() {
        String user = null;
        Authentication authentication = null;
        try {
            authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                user = (String) authentication.getCredentials();
            }
        } catch (NoClassDefFoundError cnfe) {
            try {
                // @todo fix once upgraded to spring3
                Class clazz = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                Object context = clazz.getMethod("getContext").invoke(null);
                Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
                user = (String) auth.getClass().getMethod("getCredentials").invoke(auth);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        if (user == null) {
            user = "anonymous";
        }
        return user;
    }

    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("import");
        if (i != null) {
            ImportContext context = null;
            try {
                context = importer.getContext(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (context == null && mustExist) {
                throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return context;
        }
        else {
            if (allowAll) {
                Form form = getRequest().getResourceRef().getQueryAsForm();
                if (form.getNames().contains("all")) {
                    return importer.getAllContexts();
                } else {
                    return importer.getContextsByUser(getCurrentUser());
                }
            }
            throw new RestletException("No import specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    class ImportContextJSONFormat extends StreamDataFormat {

        XStreamPersister xp;

        public ImportContextJSONFormat() {
            super(MediaType.APPLICATION_JSON);
            xp = new XStreamPersisterFactory().createJSONPersister();
            xp.setReferenceByName(true);
            xp.setExcludeIds();
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);
            return json.context(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);

            PageInfo pageInfo = getPageInfo();
            // @hack lop off query if there is one or resulting URIs look silly
            int queryIdx = pageInfo.getPagePath().indexOf('?');
            if (queryIdx > 0) {
                pageInfo.setPagePath(pageInfo.getPagePath().substring(0, queryIdx));
            }
            if (object instanceof ImportContext) {
                json.context((ImportContext) object, pageInfo, out);
            }
            else {
                json.contexts((Iterator<ImportContext>)object, pageInfo, out);
            }
        }
    }
}
