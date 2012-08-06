package org.opengeo.data.importer.rest;

import java.io.File;

import javax.servlet.ServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext.State;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.ImporterTestSupport;
import org.opengeo.data.importer.SpatialFile;

import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.InputStream;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengeo.data.importer.*;
import org.restlet.data.Status;

public class ItemResourceTest extends ImporterTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        importer.createContext(new Directory(dir));
    }

    public void testGetAllItems() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/items");

        JSONArray items = json.getJSONArray("items");
        assertEquals(2, items.size());

        JSONObject item = items.getJSONObject(0);
        assertEquals(0, item.getInt("id"));
        assertTrue(item.getString("href").endsWith("/imports/0/tasks/0/items/0"));
        
        item = items.getJSONObject(1);
        assertEquals(1, item.getInt("id"));
        assertTrue(item.getString("href").endsWith("/imports/0/tasks/0/items/1"));
    }

    public void testGetItem() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/items/0");
        JSONObject item = json.getJSONObject("item");
        
        assertEquals(0, item.getInt("id"));
        assertTrue(item.getString("href").endsWith("/imports/0/tasks/0/items/0"));
    }
    
    private String createSRSJSON(String srs) {
        return "{" + 
          "\"resource\": {" + 
            "\"featureType\":   {" + 
               "\"srs\": \"" + srs + "\"" +
             "}" + 
           "}" + 
        "}";
    }
    
    private void verifyInvalidCRSErrorResponse(MockHttpServletResponse resp) {
        assertEquals(Status.CLIENT_ERROR_BAD_REQUEST.getCode(), resp.getStatusCode());
        JSONObject errorResponse = JSONObject.fromObject(resp.getOutputStreamContent());
        JSONArray errors = errorResponse.getJSONArray("errors");
        assertTrue(errors.get(0).toString().startsWith("Invalid SRS"));
    }

    public void testPutItemSRS() throws Exception {
        File dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/1/tasks/0/items/0");
        JSONObject item = json.getJSONObject("item");
        assertEquals("NO_CRS", item.get("state"));
        assertFalse(item.getJSONObject("resource").getJSONObject("featureType").containsKey("srs"));

        // verify invalid SRS handling
        String srsRequest = createSRSJSON("26713");
        MockHttpServletResponse resp = putAsServletResponse("/rest/imports/1/tasks/0/items/0", srsRequest, "application/json");
        verifyInvalidCRSErrorResponse(resp);
        srsRequest = createSRSJSON("EPSG:9838275");
        resp = putAsServletResponse("/rest/imports/1/tasks/0/items/0", srsRequest, "application/json");
        verifyInvalidCRSErrorResponse(resp);
        
        srsRequest = createSRSJSON("EPSG:26713");
        put("/rest/imports/1/tasks/0/items/0", srsRequest, "application/json");
        
        ImportContext context = importer.getContext(1);
        ReferencedEnvelope latLonBoundingBox = context.getTasks().get(0).getItems().get(0).getLayer().getResource().getLatLonBoundingBox();
        assertNotNull(latLonBoundingBox);

        json = (JSONObject) getAsJSON("/rest/imports/1/tasks/0/items/0");
        item = json.getJSONObject("item");
        assertEquals("READY", item.get("state"));
        assertEquals("EPSG:26713", 
            item.getJSONObject("resource").getJSONObject("featureType").getString("srs"));
        State state = context.getState();
        assertEquals("Invalid context state", State.READY, state);
    }

    public void testDeleteItem() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("/rest/imports/0/tasks/0/items/0");
        assertEquals(200, response.getStatusCode());

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0/tasks/0/items");

        JSONArray items = json.getJSONArray("items");
        assertEquals(1, items.size());
        assertEquals(1, items.getJSONObject(0).getInt("id"));
    }
}
