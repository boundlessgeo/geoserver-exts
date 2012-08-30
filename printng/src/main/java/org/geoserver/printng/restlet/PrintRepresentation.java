package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.restlet.resource.StreamRepresentation;

public class PrintRepresentation extends StreamRepresentation {

    private final PrintngFacade facade;

    public PrintRepresentation(PrintngFacade facade) {
        super(facade.getMediaType());
        this.facade = facade;
    }

    @Override
    public InputStream getStream() throws IOException {
        throw new UnsupportedOperationException("PrintRepresentation getStream");
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        facade.writeTo(outputStream);
    }

}
