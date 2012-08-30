package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

public class PDFWriter implements PrintngWriter {

    @Override
    public void write(Document document, PrintSpec spec, OutputStream out) throws IOException {
        ITextRenderer renderer = new ITextRenderer();
        SharedContext sharedContext = renderer.getSharedContext();
        String baseURL = spec.getBaseURL();
        if (baseURL != null && !baseURL.isEmpty()) {
            sharedContext.setBaseURL(baseURL);
        }
        Integer dotsPerPixel = spec.getDotsPerPixel();
        if (dotsPerPixel != null && dotsPerPixel > 0) {
            sharedContext.setDotsPerPixel(dotsPerPixel);
        }
        renderer.setDocument(document, baseURL);
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (DocumentException ex) {
            throw new IOException("Error rendering PDF", ex);
        }
    }

}
