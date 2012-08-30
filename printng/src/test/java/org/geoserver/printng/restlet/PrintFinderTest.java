package org.geoserver.printng.restlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.geoserver.printng.restlet.PrintFinder;
import org.geoserver.rest.RestletException;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class PrintFinderTest {

    @Test
    public void testFindTargetRequestResponse() {
        PrintFinder printFinder = new PrintFinder(null);
        Reference resourceRef = new Reference();
        resourceRef.setPath("/unused");
        Request request = new Request();
        request.setResourceRef(resourceRef);
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("ext", "png");
        Response response = new Response(request);
        Resource resource = printFinder.findTarget(request, response);
        assertNotNull(resource);
    }

    @Test
    public void testFindTargetNoExtension() throws Exception {
        PrintFinder printFinder = new PrintFinder(null);
        Reference resourceRef = new Reference();
        resourceRef.setPath("/unused");
        Request request = new Request();
        Map<String, Object> attributes = request.getAttributes();
        attributes.put("ext", "unknown");
        request.setResourceRef(resourceRef);
        Response response = new Response(request);
        try {
            printFinder.findTarget(request, response);
        } catch (RestletException e) {
            assertTrue(true);
            return;
        }
        fail("Should have thrown restlet exception");
    }

}
