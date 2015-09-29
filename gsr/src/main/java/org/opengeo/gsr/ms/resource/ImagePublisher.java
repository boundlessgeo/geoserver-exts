/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.resource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractURLPublisher;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.opengeo.gsr.core.renderer.ClassBreaksRenderer;
import org.opengeo.gsr.core.renderer.Renderer;
import org.opengeo.gsr.core.renderer.SimpleRenderer;
import org.opengeo.gsr.core.renderer.StyleEncoder;
import org.opengeo.gsr.core.renderer.UniqueValueRenderer;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.servlet.ModelAndView;

/**
 * Either serve a generated image or a file from the style directory.
 * 
 * @author Ian Schneider <ischneider@boundlessgeo.com>
 */
public class ImagePublisher extends AbstractURLPublisher {

    final GeoServer geoserver;
    final GeoServerResourceLoader loader;
    ServletContextResourceLoader scloader;

    public ImagePublisher(GeoServerResourceLoader loader, GeoServer geoserver) {
        this.loader = loader;
        this.geoserver = geoserver;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String path = (String) request.getRequestURI();

        int index = path.lastIndexOf('/');
        String fileName = index < 0 ? path : path.substring(index + 1);

        if (fileName.startsWith("gsr:")) {
            // gsr:LayerInfoImpl--570ae188:124761b8d78:-7fc6 with optional :idx at end
            String[] parts = fileName.split(":");
            Catalog catalog = geoserver.getCatalog();
            // @todo more checks for invalid requests, tests
            LayerInfo lyr = catalog.getLayer(fileName.substring(4, parts.length == 4 ? fileName.length() : fileName.length() - parts[4].length() - 1));
            Integer idx = parts.length == 5 ? Integer.parseInt(parts[4]) : null;
            Renderer renderer = StyleEncoder.effectiveRenderer(lyr);
            BufferedImage image = null;
            if (renderer instanceof SimpleRenderer) {
                SimpleRenderer simpleRenderer = (SimpleRenderer) renderer;
                image = LegendResource.render(simpleRenderer.getSymbol());
            } else if (renderer instanceof ClassBreaksRenderer) {
                ClassBreaksRenderer classBreaksRenderer = (ClassBreaksRenderer) renderer;
                image = LegendResource.render(classBreaksRenderer.getClassBreakInfos().get(idx).getSymbol());
            } else if (renderer instanceof UniqueValueRenderer) {
                UniqueValueRenderer uniqueValueRenderer = (UniqueValueRenderer) renderer;
                image = LegendResource.render(uniqueValueRenderer.getUniqueValueInfos().get(idx).getSymbol());
            }
            if (image != null) {
                byte[] body = LegendResource.toPNGBytes(image);
                response.setHeader("Content-Type", "image/png");
                response.setHeader("Content-Length", Integer.toString(body.length));
                response.getOutputStream().write(body);
            }
            return null;
        } else {
            return super.handleRequestInternal(request, response);
        }
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        this.scloader = new ServletContextResourceLoader(servletContext);
    }

    @Override
    protected URL getUrl(HttpServletRequest request) throws IOException {
        String reqPath = request.getRequestURI();
        int idx = reqPath.indexOf("images/");
        if (idx < 0) {
            return null;
        }
        reqPath = "styles/" + reqPath.substring(idx + 7); // lucky seven

        File file = loader.find(reqPath);

        if (file == null && scloader != null) {
            // try loading as a servlet resource
            ServletContextResource resource = (ServletContextResource) scloader
                    .getResource(reqPath);
            if (resource != null && resource.exists()) {
                file = resource.getFile();
            }
        }

        if(file != null) {
            return DataUtilities.fileToURL(file);
        } else {
            return null;
        }
    }

}
