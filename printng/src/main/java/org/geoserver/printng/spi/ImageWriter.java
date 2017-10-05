package org.geoserver.printng.spi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.printng.PrintSupport;
import org.geoserver.printng.PrintUserAgentCallback;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;
import org.xhtmlrenderer.util.FSImageWriter;

public class ImageWriter extends PrintngWriter {
    
    private final String format;
    
    public ImageWriter(String format) {
        this.format = format;
    }

    @Override
    public String getExtension() {
        return format;
    }

    @Override
    protected void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        Document document = spec.getDocument();
        // the image renderer requires a dimension to initially render at
        // if not already provided, try to get one
        if (!spec.isRenderDimensionSet()) {
            // this searches CSS for a style element with width/height
            PrintSpecDocumentConfigurator.configure(spec, spec.getDocument());
            // dang, do something
            if (!spec.isRenderDimensionSet()) {
                Logging.getLogger(getClass()).warning("using default render dimensions");
                spec.useDefaultRenderDimension();
            }
        }
        Java2DRenderer renderer = new Java2DRenderer(document, spec.getWidth(), spec.getHeight());
        SharedContext context = renderer.getSharedContext();
        spec.setDotsPerPixel(-1); // don't take any values here
        configure(context, spec);

        FSImageWriter writer = new FSImageWriter(format);
        BufferedImage image = renderer.getImage();
        if (spec.isOutputDimensionSet()
                && image.getWidth() != spec.getOutputWidth()
                && image.getHeight() != spec.getOutputHeight()) {
            image = PrintSupport.niceImage(image, spec.getOutputWidth(), spec.getOutputHeight(), true);
        }
        writer.write(image, out);
    }

    @Override
    protected void configureInternal(SharedContext context, PrintUserAgentCallback callback) {
        /**
         * The renderer internally uses a separate path to resolve image
         * resources, so this makes it use any images loaded by the
         * PrintUserAgentCallback
         */
        context.setReplacedElementFactory(new SwingReplacedElementFactory(
                ImageResourceLoader.NO_OP_REPAINT_LISTENER, callback.createImageResourceLoader()));
    }
    
    
}
