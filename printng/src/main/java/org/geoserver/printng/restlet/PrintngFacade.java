package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.printng.spi.DocumentParser;
import org.geoserver.printng.spi.PrintSpecException;
import org.geoserver.rest.RestletException;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;

public class PrintngFacade {

    private final Request request;

    private final Response response;

    private final ReaderSource readerSource;

    private final OutputDescriptor outputDescriptor;

    private volatile Document document;

    public PrintngFacade(Request request, Response response, ReaderSource readerSource) {
        this.request = request;
        this.response = response;
        this.readerSource = readerSource;

        String extension = request.getAttributes().get("ext").toString().toLowerCase();
        outputDescriptor = new OutputDescriptor(extension);
        // document is lazily parsed when required
        document = null;
    }

    private Document parseDocument() {
        PrintngReader printngReader = readerSource.printngReader(request);
        Reader reader = null;
        try {
            reader = printngReader.reader();
            DocumentParser documentParser = new DocumentParser(reader);
            return documentParser.parse();
        } catch (IOException e) {
            throw new RestletException("Error reading input", Status.SERVER_ERROR_INTERNAL, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public PrintResource getResource() {
        return new PrintResource(this);
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public Variant getVariant() {
        return outputDescriptor.getVariant();
    }

    public PrintRepresentation getRepresentation(Variant variant) {
        return new PrintRepresentation(this);
    }

    public MediaType getMediaType() {
        return getVariant().getMediaType();
    }

    public PrintngWriter getWriter() {
        return outputDescriptor.getWriter();
    }

    public Document readDocument() {
        if (document == null) {
            synchronized (this) {
                if (document == null) {
                    document = parseDocument();
                }
            }
        }
        return document;
    }

    public PrintSpec getPrintSpec() {
        return new RequestPrintSpec(request);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        Document document = readDocument();
        PrintngWriter writer = getWriter();
        PrintSpec printSpec = getPrintSpec();
        try {
            writer.write(document, printSpec, outputStream);
        } catch (PrintSpecException e) {
            throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
    }

}
