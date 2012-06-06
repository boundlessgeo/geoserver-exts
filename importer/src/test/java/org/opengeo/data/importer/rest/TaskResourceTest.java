package org.opengeo.data.importer.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.data.util.IOUtils;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;
import org.opengeo.data.importer.DataFormat;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.GridFormat;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.ImporterTestSupport;
import org.opengeo.data.importer.SpatialFile;
import org.opengeo.data.importer.VFSWorker;
import org.opengeo.data.importer.transform.CreateIndexTransform;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * @todo extract postgis stuff to online test case
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class TaskResourceTest extends ImporterTestSupport {
    JDBCDataStore jdbcStore;

    // some rest calls now require admin permissions
    private void doLogin() throws Exception {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        l.add(new GeoServerRole("ROLE_ADMINISTRATOR"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "geoserver", l));
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        doLogin();

        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);
        importer.createContext(new Directory(dir));

        DataStoreInfoImpl postgis = new DataStoreInfoImpl(getCatalog());
        postgis.setName("postgis");
        postgis.setType("PostGIS");
        postgis.setEnabled(true);
        postgis.setWorkspace(getCatalog().getDefaultWorkspace());
        Map<String,Serializable> params = new HashMap<String, Serializable>();
        params.put("port",5432);
        params.put("passwd","geonode");
        params.put("dbtype","postgis");
        params.put("host","localhost");
        params.put("database","geonode_imports");
        params.put("namespace", "http://geonode.org");
        params.put("schema", "public");
        params.put("user", "geonode");
        postgis.setConnectionParameters(params);
        getCatalog().add(postgis);
        try {
            // force connection - will fail if we cannot connect
            postgis.getDataStore(null).getNames();
            jdbcStore = (JDBCDataStore) postgis.getDataStore(null);
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING,"Could not initialize postgis db",ioe);
        }
    }
    
    private Integer putZip(String path) throws Exception {
        File file = new File(path);
        InputStream stream;
        if (file.exists()) {
            stream = new FileInputStream(file);
        } else {
            stream = ImporterTestSupport.class.getResourceAsStream("../test-data/" + path);
        }
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/" + file.getName());
        req.setContentType("application/zip");
        req.addHeader("Content-Type","application/zip");
        req.setMethod("PUT");
        req.setBodyContent(org.apache.commons.io.IOUtils.toByteArray(stream));
        resp = dispatch(req);
        
        assertEquals(201, resp.getStatusCode());
        
        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals(ImportTask.State.READY, task.getState());
        
        return id;
    }
    
    private Integer putZipAsURL(String zip) throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);
        
        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/");
        Form form = new Form();
        form.add("url", new File(zip).getAbsoluteFile().toURI().toString());
        req.setBodyContent(form.encode());
        req.setMethod("POST");
        req.setContentType(MediaType.APPLICATION_WWW_FORM.toString());
        req.setHeader("Content-Type", MediaType.APPLICATION_WWW_FORM.toString());
        resp = dispatch(req);
        
        assertEquals(201, resp.getStatusCode());
        
        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals(ImportTask.State.READY, task.getState());
        
        return id;
    }
    
    Integer upload(String zip, boolean asURL) throws Exception {
        URL resource = ImporterTestSupport.class.getResource("../test-data/" + zip);
        File file = new File(resource.getFile());
        String[] nameext = file.getName().split("\\.");
        Connection conn = jdbcStore.getConnection(Transaction.AUTO_COMMIT);
        String sql = "drop table if exists \"" + nameext[0] + "\"";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);
        stmt.close();
        conn.close();
        if (asURL) {
            // make a copy since, zip as url will archive and delete it
            File copyDir = tmpDir();
            FileUtils.copyFile(file, new File(copyDir,zip));
            return putZipAsURL(new File(copyDir,zip).getAbsolutePath());
        } else {
            return putZip(zip);
        }
    }
    
    public void testUploadToPostGISViaFile() throws Exception {
        if (jdbcStore == null) return;
        
        Integer id = upload("shape/archsites_epsg_prj.zip", true);
        completeAndVerifyUpload(id);
    }


    public void testUploadToPostGISViaZip() throws Exception {
        if (jdbcStore == null) return;
        
        Integer id = upload("shape/archsites_epsg_prj.zip", false);
        completeAndVerifyUpload(id);
    }
        
    void completeAndVerifyUpload(Integer id) throws Exception {
        
        JSONObjectBuilder builder = new JSONObjectBuilder();
        builder.object().key("task").object()
          .key("target").object()
            .key("dataStore").object()
              .key("name").value("postgis")
              .key("workspace").object()
                .key("name").value(getCatalog().getDefaultWorkspace().getName())
              .endObject()
            .endObject()
          .endObject()
        .endObject().endObject();
                
        String payload = builder.buildObject().toString();
        
        MockHttpServletResponse resp = putAsServletResponse("/rest/imports/" + id + "/tasks/0", payload, "application/json");
        assertEquals(204,resp.getStatusCode());
        
        // @todo formalize and extract this test
        ImportContext context = importer.getContext(id);
        context.getTasks().get(0).getItems().get(0).getTransform().add(new CreateIndexTransform("CAT_ID"));
        importer.changed(context);
        
        resp = postAsServletResponse("/rest/imports/" + id,"","application/text");
        assertEquals(204,resp.getStatusCode());
        
        // ensure item ran successfully
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/" + id + "/tasks/0/items/0");
        json = json.getJSONObject("item");
        assertEquals("COMPLETE",json.get("state"));
        
        json = (JSONObject) getAsJSON("/rest/workspaces/" + getCatalog().getDefaultWorkspace().getName() + "/datastores/postgis/featuretypes.json");
        // make sure the new feature type exists
        JSONObject featureTypes = (JSONObject) json.get("featureTypes");
        JSONArray featureType = (JSONArray) featureTypes.get("featureType");
        JSONObject type = (JSONObject) featureType.get(0);
        // @todo why is generated type name possibly getting a '0' appended to it when we drop the table???
        assertTrue(type.getString("name").startsWith("archsites"));
        
        File archive = importer.getArchiveFile(context.getTasks().get(0));
        assertTrue(archive.exists());
        
        // @todo do it again and ensure feature type is created
        // correctly w/ auto-generated name 
    }

    public void testGetAllTasks() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks");

        JSONArray tasks = json.getJSONArray("tasks");
        assertEquals(2, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/0"));
        
        task = tasks.getJSONObject(1);
        assertEquals(1, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/1"));
    }

    public void testGetTask() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0");
        JSONObject task = json.getJSONObject("task");
        assertEquals(0, task.getInt("id"));
        assertTrue(task.getString("href").endsWith("/imports/0/tasks/0"));
    }

    public void testPostMultiPartFormData() throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);
        assertNull(context.getData());
        assertTrue(context.getTasks().isEmpty());

        File dir = unpack("shape/archsites_epsg_prj.zip");
        
        Part[] parts = new Part[]{new FilePart("archsites.shp", new File(dir, "archsites.shp")), 
            new FilePart("archsites.dbf", new File(dir, "archsites.dbf")), 
            new FilePart("archsites.shx", new File(dir, "archsites.shx")), 
            new FilePart("archsites.prj", new File(dir, "archsites.prj"))};

        MultipartRequestEntity multipart = 
            new MultipartRequestEntity(parts, new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setBodyContent(bout.toByteArray());
        resp = dispatch(req);

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals(ImportTask.State.READY, task.getState());
    }
    
    private void uploadGeotiffAndVerify(String taskName,
            InputStream geotiffResourceStream, String contentType) throws Exception {
        // upload  tif or zip file containing a tif and verify the results
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull(resp.getHeader("Location"));

        String[] split = resp.getHeader("Location").split("/");
        Integer id = Integer.parseInt(split[split.length-1]);
        ImportContext context = importer.getContext(id);

        MockHttpServletRequest req = createRequest("/rest/imports/" + id + "/tasks/" + taskName);
        req.setContentType(contentType);
        req.addHeader("Content-Type", contentType);
        req.setMethod("PUT");
        req.setBodyContent(org.apache.commons.io.IOUtils.toByteArray(geotiffResourceStream));
        resp = dispatch(req);

        assertEquals(201, resp.getStatusCode());

        context = importer.getContext(context.getId());
        assertNull(context.getData());
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());

        ImportData importData = task.getData();
        assertTrue(importData instanceof SpatialFile);

        DataFormat format = importData.getFormat();
        assertTrue(format instanceof GridFormat);

        List<ImportItem> items = task.getItems();
        assertEquals(1, items.size());
        ImportItem importItem = items.get(0);
        assertEquals(ImportItem.State.READY, importItem.getState());
    }

    public void testPostGeotiffBz2() throws Exception {
        String path = "geotiff/EmissiveCampania.tif.bz2";
        InputStream stream = ImporterTestSupport.class.getResourceAsStream("../test-data/" + path);

        uploadGeotiffAndVerify(new File(path).getName(), stream, "application/x-bzip2");
    }


    public void testPostGeotiff() throws Exception {
        File tempBase = tmpDir();
        File tempDir = new File(tempBase, "testPostGeotiff");
        if (!tempDir.mkdirs()) {
            throw new IllegalStateException("Cannot create temp dir for testing geotiff");
        }

        String tifname = "EmissiveCampania.tif";
        String bz2name = tifname + ".bz2";
        File destinationArchive = new File(tempDir, bz2name);
        InputStream inputStream = ImporterTestSupport.class.getResourceAsStream("../test-data/geotiff/" + bz2name);

        IOUtils.copy(inputStream, destinationArchive);

        VFSWorker vfs = new VFSWorker();
        vfs.extractTo(destinationArchive, tempDir);

        File tiff = new File(tempDir, tifname);
        if (!tiff.exists()) {
            throw new IllegalStateException("Did not extract tif correctly");
        }

        FileInputStream fis = new FileInputStream(tiff);
        uploadGeotiffAndVerify(tifname, fis, "image/tiff");
    }
}
