/* Copyright (c) 2001 - 2013 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 *
 * @author tkunicki
 */
public class ImageResourceController extends AbstractController implements ApplicationContextAware {
    
    public static final String PROPERTY_IMAGE_RESOURCE_DIR = "GSR_IMAGE_RESOURCE_DIR";
    
    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    private static final String HTTP_HEADER_ETAG = "ETag";
    private static final String HTTP_HEADER_CACHE_CONTROL = "Cache-Control";

    private static final Map<String, String> defaultMimeTypes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    {
        defaultMimeTypes.put(".gif", "image/gif");
        defaultMimeTypes.put(".jpeg", "image/jpeg");
        defaultMimeTypes.put(".jpg", "image/jpeg");
        defaultMimeTypes.put(".png", "image/png");
    }

    private final File imageBaseDirectory;

    public ImageResourceController(GeoServer geoserver, ServletContext context) {
        this.imageBaseDirectory = findImageResourceDirectory(geoserver, context);
    }

    @Override
    public ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {

        final String path = (String) request.getRequestURI();

        int index = path.lastIndexOf('/');
        String fileName = index < 0 ? path : path.substring(index + 1);

        dispatchImageResource(fileName, request, response);

        return null;
    }

    public boolean dispatchImageResource(final String fileName, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final boolean debug = logger.isDebugEnabled();
        if (debug) {
            logger.debug("Attemping to dispatch image resource: " + fileName);
        }

        boolean resolved = false;
        if (imageBaseDirectory == null) {
            logger.warn("Unable to dispatch image resource, the property " + PROPERTY_IMAGE_RESOURCE_DIR + " is not set.");
        } else {
            File imageFile = new File(imageBaseDirectory, fileName);
            if (imageFile.canRead()) {
                try {
                    commitResponse(imageFile, response);
                    resolved = true;
                } catch (IOException e) {
                    logger.warn("Error dispatching image resource response", e);
                }
            } else {
                logger.warn("Error dispatching image resource response, " + imageFile.getPath() + " not found.");
            }
        }
        return resolved;
    }

    public void commitResponse(File imageFile, HttpServletResponse response)
            throws IOException {
        writeHeaders(imageFile, response);
        writeImageData(imageFile, response);

    }

    protected void writeHeaders(File imageFile, HttpServletResponse response) {

        // determine mimetype
        String imagePath = imageFile.getPath();
        String mimetype = getServletContext().getMimeType(imagePath);
        if (mimetype == null) {
            final int extIndex = imagePath.lastIndexOf('.');
            if (extIndex != -1) {
                String extension = imagePath.substring(extIndex);
                mimetype = (String) defaultMimeTypes.get(extension.toLowerCase());
            }
        }

        long length = imageFile.length();
        long lastModified = imageFile.lastModified();

        response.setContentType(mimetype);
        response.setHeader(HTTP_HEADER_CONTENT_LENGTH, Long.toString(length));
        if (lastModified != 0) {
            response.setHeader(HTTP_HEADER_ETAG, '"' + Long.toString(lastModified) + '"');
            response.setDateHeader(HTTP_HEADER_LAST_MODIFIED, lastModified);
        }
        if (!response.containsHeader(HTTP_HEADER_CACHE_CONTROL)) {
            response.setHeader(HTTP_HEADER_CACHE_CONTROL, "max-age=86400");
        }
    }

    protected void writeImageData(File imageFile, HttpServletResponse response) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(imageFile);
            os = response.getOutputStream();
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }
    
    private File findImageResourceDirectory(GeoServer geoserver, ServletContext context) {
        
        // list of accessors, order implies priority
        List<PropertyAccessor> accessors = Arrays.asList(
                new SystemPropertyAccessor(),
                new ServletContextInitParameterAccessor(context),
                new EnvironmentVariableAccessor(),
                new DefaultValueAccessor(geoserver));
        
        for (PropertyAccessor accessor : accessors) {
            String path = accessor.get(PROPERTY_IMAGE_RESOURCE_DIR);
            if (path == null){
                logger.trace(accessor.source() + " " + PROPERTY_IMAGE_RESOURCE_DIR + " is not set");
            } else {
                File file = new File(path);

                StringBuilder messageBuilder = new StringBuilder().
                        append(accessor.source()).
                        append(' ').
                        append(PROPERTY_IMAGE_RESOURCE_DIR).
                        append(" set to ").append(path);
                if (!file.exists()) {
                    logger.warn(messageBuilder.append(" , but this path does not exist").toString());
                } else if (!file.isDirectory()) {
                    logger.warn(messageBuilder.append(" , which is not a directory").toString());
                } else if (!file.canWrite()) {
                    logger.warn(messageBuilder.append(" , which is not writeable").toString());
                } else {
                    logger.info(messageBuilder.toString());
                    return file;
                }
            }
        }
        return null;
    }
    
    interface PropertyAccessor {
        public String get(String propertyName);
        public String source();
    }
    
    private static class SystemPropertyAccessor implements PropertyAccessor {
        @Override public String get(String propertyName) {
            return System.getProperty(propertyName);
        }
        @Override public String source() { return "System property"; }
    }
    
    private static class ServletContextInitParameterAccessor implements PropertyAccessor {
        private final ServletContext context;
        public ServletContextInitParameterAccessor(ServletContext context) {
            this.context = context;
        }
        @Override public String get(String propertyName) {
            return context == null ? null : context.getInitParameter(propertyName);
        }
        @Override public String source() { return "Servlet context parameter"; }
    }
    
    private static class EnvironmentVariableAccessor implements PropertyAccessor {
        @Override public String get(String propertyName) {
            return System.getenv(propertyName);
        }
        @Override public String source() { return "Environment variable"; }
    }
    
    private static class DefaultValueAccessor implements PropertyAccessor {
        private final GeoServer geoserver;
        public DefaultValueAccessor(GeoServer geoserver) {
            this.geoserver = geoserver;
        }
        @Override public String get(String propertyName) {
            return geoserver == null ? null : geoserver.getCatalog().
                    getResourceLoader().
                    getBaseDirectory().
                    getAbsolutePath() + File.separator + "images";
        }
        @Override public String source() { return "Default value for"; }
    }
    
}
