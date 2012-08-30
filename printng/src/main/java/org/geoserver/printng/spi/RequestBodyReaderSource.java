package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Status;

public class RequestBodyReaderSource implements ReaderSource {

    @Override
    public PrintngReader printngReader(Request request) {
        InputStream inputStream;
        try {
            inputStream = request.getEntity().getStream();
        } catch (IOException e) {
            throw new RestletException("Invalid input stream", Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
        return new TemplatePrintngReader(inputStream);
    }

    private static class TemplatePrintngReader implements PrintngReader {

        private final InputStream inputStream;

        private TemplatePrintngReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public Reader reader() throws IOException {
            return new InputStreamReader(inputStream);
        }

    }
}
