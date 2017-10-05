package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

public class PDFWriter extends PrintngWriter {

    @Override
    public String getExtension() {
        return "pdf";
    }

    @Override
    public void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        ITextRenderer renderer = new ITextRenderer();
        configure(renderer.getSharedContext(), spec);
        renderer.setDocument(spec.getDocument(), spec.getBaseURL());
        renderer.layout();
        try {
            renderer.createPDF(out);
        } catch (DocumentException ex) {
            throw new IOException("Error rendering PDF", ex);
        }
    }

}
