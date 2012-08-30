package org.geoserver.printng.restlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geoserver.printng.FreemarkerSupport;
import org.geoserver.printng.restlet.FreemarkerTemplateResource;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class FreemarkerTemplateResourceTest extends GeoServerTestSupport {

    @Test
    public void testHandlePost() throws FileNotFoundException, IOException {
        Request request = new Request();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("template", "foo");
        request.setEntity("<div>foobar</div>", MediaType.TEXT_HTML);
        Response response = new Response(request);
        FreemarkerTemplateResource freemarkerTemplateResource = new FreemarkerTemplateResource(
                request, response);
        freemarkerTemplateResource.handlePost();
        assertEquals("Invalid response", Status.SUCCESS_OK, response.getStatus());

        File directory = FreemarkerSupport.getPrintngTemplateDirectory();
        File template = new File(directory, "foo.ftl");
        assertTrue("template wasn't created", template.exists());
        String contents = IOUtils.toString(new FileReader(template));
        assertEquals("Invalid template contents",
                "<?xml version=\"1.0\"?>\n<html><body><div>foobar</div></body></html>", contents);
    }

}
