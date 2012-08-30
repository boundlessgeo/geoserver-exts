package org.geoserver.printng.restlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.rest.RestletException;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class PrintngFacadeTest {

    @Test
    public void testUnknownExtension() {
        Request request = new Request();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("ext", "foo");
        try {
            new PrintngFacade(request, new Response(request), null);
            fail("restlet exception should have been thrown");
        } catch (RestletException e) {
            int code = e.getStatus().getCode();
            int expCode = Status.CLIENT_ERROR_NOT_FOUND.getCode();
            assertEquals("Invalid reslet status code", expCode, code);
        }
    }

    @Test
    public void testWriteToSuccessful() throws IOException {
        Request request = new Request();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("ext", "png");
        Reference reference = new Reference();
        reference.setPath("/unused");
        reference.setQuery("width=10&height=5");
        request.setResourceRef(reference);
        String input = "<div>foo bar</div>";
        ReaderSource readerSource = mock(ReaderSource.class);
        PrintngReader printngReader = mock(PrintngReader.class);
        when(printngReader.reader()).thenReturn(new StringReader(input));
        when(readerSource.printngReader(request)).thenReturn(printngReader);
        PrintngFacade printngFacade = new PrintngFacade(request, new Response(request),
                readerSource);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        printngFacade.writeTo(byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        assertTrue("bytes not written", byteArray.length > 0);
    }

    @Test
    public void testWriteToInvalidPrintSpec() throws IOException {
        Request request = new Request();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("ext", "png");
        Reference reference = new Reference();
        reference.setPath("/unused");
        reference.setQuery("");
        request.setResourceRef(reference);
        String input = "<div>foo bar</div>";
        ReaderSource readerSource = mock(ReaderSource.class);
        PrintngReader printngReader = mock(PrintngReader.class);
        when(printngReader.reader()).thenReturn(new StringReader(input));
        when(readerSource.printngReader(request)).thenReturn(printngReader);
        PrintngFacade printngFacade = new PrintngFacade(request, new Response(request),
                readerSource);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            printngFacade.writeTo(byteArrayOutputStream);
            fail("RestletException should have been thrown");
        } catch (RestletException e) {
            int expCode = Status.CLIENT_ERROR_BAD_REQUEST.getCode();
            int code = e.getStatus().getCode();
            assertEquals("Invalid status code", expCode, code);
        }
    }

}
