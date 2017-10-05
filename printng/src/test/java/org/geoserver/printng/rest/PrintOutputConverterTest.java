package org.geoserver.printng.rest;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PrintOutputConverterTest  extends GeoServerSystemTestSupport {

    @Test
    public void testUnknownExtension() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/rest/printng/render.foo", "<div>foo bar</div>", MediaType.TEXT_HTML_VALUE);
        assertEquals("Rest exception should have been thrown", HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void testWriteToSuccessful() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/rest/printng/render.png?width=10&height=5", "<div>foo bar</div>", MediaType.TEXT_HTML_VALUE);
        assertEquals("invalid response", HttpStatus.OK.value(), response.getStatus());
        assertTrue("bytes not written", response.getContentAsByteArray().length > 0);
    }
}
