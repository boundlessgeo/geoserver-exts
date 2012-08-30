package org.geoserver.printng.restlet;

import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.PDFWriter;
import org.geoserver.rest.RestletException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

public class OutputDescriptor {

    private Variant variant;

    private PrintngWriter writer;

    public OutputDescriptor(String extension) {
        if ("pdf".equals(extension)) {
            variant = new Variant(MediaType.APPLICATION_PDF);
            writer = new PDFWriter();
        } else if ("jpg".equals(extension)) {
            variant = new Variant(MediaType.IMAGE_JPEG);
            writer = new ImageWriter("jpg");
        } else if ("png".equals(extension)) {
            variant = new Variant(MediaType.IMAGE_PNG);
            writer = new ImageWriter("png");
        } else if ("gif".equals(extension)) {
            variant = new Variant(MediaType.IMAGE_GIF);
            writer = new ImageWriter("gif");
        } else {
            String error = String.format("Unknown rendering extension \"%s\"", extension);
            throw new RestletException(error, Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    public Variant getVariant() {
        return variant;
    }

    public PrintngWriter getWriter() {
        return writer;
    }

}
