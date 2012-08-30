package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.geoserver.printng.FreemarkerSupport;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.restlet.data.Status;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RestEndpointTest extends GeoServerTestSupport {

    @Test
    public void testValidPngRender() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/rest/printng/render.png",
                "<div>FOOBAR</div>");
        assertEquals("Invalid response", Status.SUCCESS_OK.getCode(), response.getStatusCode());
    }

    @Test
    public void testValidPngFreemarkerTemplateRender() throws Exception {
        createPrintngFreemarkerTemplate("foo.ftl", "<div>${foo}</div>");
        MockHttpServletResponse response = postAsServletResponse(
                "/rest/printng/freemarker/foo.png?foo=bar", "");
        assertEquals("Invalid response", Status.SUCCESS_OK.getCode(), response.getStatusCode());
    }

    private void createPrintngFreemarkerTemplate(String templateName, String templateContents)
            throws IOException {
        Writer writer = FreemarkerSupport.newTemplateWriter(templateName);
        IOUtils.copy(new StringReader(templateContents), writer);
    }

}
