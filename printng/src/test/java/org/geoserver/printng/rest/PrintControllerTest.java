package org.geoserver.printng.rest;

import static testsupport.PrintTestSupport.assertPDF;
import static testsupport.PrintTestSupport.assertPNG;
import static testsupport.PrintTestSupport.assertTemplateExists;
import static testsupport.PrintTestSupport.map;
import junit.framework.Test;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import net.sf.json.JSONObject;
import org.geoserver.printng.GeoserverSupport;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * Put all tests that require full geoserver support here.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintControllerTest extends GeoServerTestSupport {

    String path;
    String body;
    String contentType;
    int width;
    int height;
    int status = 200;
    String[] formData;
    String requestContentType = MediaType.TEXT_HTML_VALUE;
    // see mockrunner note below
    boolean allowEmptyContentType = false;

    // make sure we don't recreate the data dir constantly
    public static Test suite() {
        return new OneTimeTestSetup(new PrintControllerTest());
    }

    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        GeoserverSupport.cleanOutput(0);
    }

    public void testValidPngRender() throws Exception {
        path = "/rest/printng/render.png";
        body = "<div>FOOBAR</div>";
        contentType = "image/png";
        runPostTest();
        runJSONPostTest();
    }
    
    public void testValidPngRenderSize() throws Exception {
        // specify native size in document
        path = "/rest/printng/render.png";
        body = "<div style='width:25px;height:25px;'>FOOBAR</div>";
        contentType = "image/png";
        width = 25;
        height = 25;
        runPostTest();
        runJSONPostTest();

        
        // now scale the output
        width = 50;
        height = 50;
        path = "/rest/printng/render.png?width=50&height=50";
        runPostTest();
        runJSONPostTest();
    }

    public void testValidPdfRender() throws Exception {
        path = "/rest/printng/render.pdf";
        body = "<div>FOOBAR</div>";
        contentType = "application/pdf";
        runPostTest();
        runJSONPostTest();
    }

    public void testValidHtmlRender() throws Exception {
        path = "/rest/printng/render.html";
        body = "<div>FOOBAR</div>";
        contentType = "text/html";
        runPostTest();
        runJSONPostTest();
    }

    public void testPOSTTemplate() throws Exception {
        // create a template
        path = "/rest/printng/freemarker/foobar";
        body = "<div>${msg}</div>";
        contentType = "text/plain";
        status = 201;
        runPostTest();
        assertTemplateExists("foobar.ftl");

        // render png w/ get params
        path = "/rest/printng/freemarker/foobar.png?msg=BAR";
        body = " ";
        contentType = "image/png";
        status = 200;
        runPostTest();

        // as html
        path = "/rest/printng/freemarker/foobar.html?msg=BAR";
        contentType = "text/html";
        MockHttpServletResponse resp = runPostTest();
        assertTrue(resp.getContentAsString().indexOf("BAR") > 0);

        // and with post body
        path = "/rest/printng/freemarker/foobar.html";
        formData = new String[]{"msg", "BAR"};
        resp = runPostTest();
        assertTrue(resp.getContentAsString().indexOf("BAR") > 0);
    }

    private MockHttpServletResponse assertPostResponse(MockHttpServletResponse resp) throws Exception {
        assertEquals(status, resp.getStatus());
        String type = resp.getContentType();
        // mockrunner (in 0.3.1 and 0.3.6) does mimetypes wrong - this makes
        // the filepublisher return an empty contenttype
        if (type == null && allowEmptyContentType) {
            type = "";
        } else {
            assertNotNull(type);
            type = type.split(";")[0];
            assertEquals(contentType, type);
        }
        if (contentType.equals("image/png")) {
            if (width == 0) {
                PrintSpec defaults = new PrintSpec(null).useDefaultRenderDimension();
                width = defaults.getOutputWidth();
                height = defaults.getOutputHeight();
            }
            assertPNG(getBinaryInputStream(resp), width, height);
        } else if (contentType.equals("application/pdf")) {
            assertPDF(getBinaryInputStream(resp));
        }
        return resp;
    }

    private String formText(MultiValueMap<String, String> form) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
            String name = nameIterator.next();
            for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext();) {
                String value = valueIterator.next();
                builder.append(URLEncoder.encode(name, "UTF-8"));
                if (value != null) {
                    builder.append('=');
                    builder.append(URLEncoder.encode(value, "UTF-8"));
                }
                if (valueIterator.hasNext()) {
                    builder.append('&');
                }
            }
            if (nameIterator.hasNext()) {
                builder.append('&');
            }
        }
        return builder.toString();
    }
    private MockHttpServletResponse runPostTest() throws Exception {
        if (formData != null) {
            body = formText(map(formData));
            requestContentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
            return assertPostResponse(postFormAsServletResponse(path, body, requestContentType));
        } else {
            return assertPostResponse(postAsServletResponse(path, body, requestContentType));
        }
    }

    //Spring uses the parameter map to read form data
    protected MockHttpServletResponse postFormAsServletResponse(String path, String body, String contentType) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(contentType);
        request.setContent(body.getBytes("UTF-8"));
        request.addHeader("Content-type", contentType);

        if (formData != null) {
            MultiValueMap<String, String> formMap = map(formData);
            for (String key : formMap.keySet()) {
                request.setParameter(key, formMap.get(key).toArray(new String[formMap.get(key).size()]));
            }
        }

        return dispatch(request);
    }
    
    private MockHttpServletResponse assertJSONPostResponse(MockHttpServletResponse resp) throws Exception {
        assertEquals(status, resp.getStatus());
        String type = resp.getContentType().split(";")[0];
        assertEquals(MediaType.APPLICATION_JSON.toString(), type);
        JSONObject obj = JSONObject.fromObject(resp.getContentAsString());
        String getURL = obj.getString("getURL");
        assertNotNull(getURL);
        assertFalse("invalid path separators", getURL.indexOf('\\') >= 0);
        // geoserver is the hard-coded context prefix
        String context = "http://localhost/geoserver";
        assertTrue("expected getURL to be prefixed by servlet context", getURL.startsWith(context));
        // adjust the getURL - the test code will add the prefix again
        getURL = getURL.replace(context, "");
        allowEmptyContentType = true; // see mockrunner note above
        assertPostResponse(getAsServletResponse(getURL));
        allowEmptyContentType = false;
        return resp;
    }
    
    private MockHttpServletResponse runJSONPostTest() throws Exception {
        if (formData != null) {
            body = formText(map(formData));
            requestContentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
        } else {
            requestContentType = MediaType.APPLICATION_JSON_VALUE;
        }
        String[] parts = path.split("[?]");
        String query = "";
        if (parts.length > 1) {
            query = parts[1];
        }
        parts = parts[0].split("\\.");
        if (query.length() > 0) {
            query += "&";
        }
        query += "format=" + parts[1];
        String jsonPath = parts[0] + ".json?" + query;
        return assertJSONPostResponse(postAsServletResponse(jsonPath, body, requestContentType));
    }
}
