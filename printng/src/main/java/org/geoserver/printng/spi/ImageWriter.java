package org.geoserver.printng.spi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.util.FSImageWriter;

public class ImageWriter implements PrintngWriter {

    private final String format;

    public ImageWriter(String format) {
        this.format = format;
    }

    @Override
    public void write(Document document, PrintSpec spec, OutputStream out) throws IOException {
        Integer width = spec.getWidth();
        Integer height = spec.getHeight();
        if (width == null || height == null) {
            throw new PrintSpecException("Invalid print spec, missing width / height");
        }
        Java2DRenderer renderer = new Java2DRenderer(document, width, height);
        SharedContext sharedContext = renderer.getSharedContext();
        String baseURL = spec.getBaseURL();
        if (baseURL != null && !baseURL.isEmpty()) {
            sharedContext.setBaseURL(baseURL);
        }
        Integer dotsPerPixel = spec.getDotsPerPixel();
        if (dotsPerPixel != null && dotsPerPixel > 0) {
            sharedContext.setDotsPerPixel(dotsPerPixel);
        }
        FSImageWriter writer = new FSImageWriter(format);
        BufferedImage image = renderer.getImage();
        writer.write(image, out);
    }

}
