package org.opengeo.data.importer.rest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.h2.H2DataStoreFactory;
import org.opengeo.data.importer.Database;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImporterTestSupport;
import org.opengeo.data.importer.SpatialFile;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import java.util.Iterator;

public class ImportResourceTest extends ImporterTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        importer.createContext(new Directory(dir));
        
        dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        importer.createContext(new Directory(dir));
        
        dir = unpack("shape/archsites_no_crs.zip");
        importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));
    }

    public void testGetAllImports() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports?all");
        assertNotNull(json.get("imports"));

        JSONArray imports = (JSONArray) json.get("imports");
        assertEquals(3, imports.size());

        JSONObject imprt = imports.getJSONObject(0);
        assertEquals(0, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/0"));

        imprt = imports.getJSONObject(1);
        assertEquals(1, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/1"));
        
        imprt = imports.getJSONObject(2);
        assertEquals(2, imprt.getInt("id"));
        assertTrue(imprt.getString("href").endsWith("/imports/2"));
    }

    public void testGetImport() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/0");

        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(0, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals("READY", task.get("state"));

        JSONObject source = task.getJSONObject("source");
        assertEquals("directory", source.getString("type"));
        assertEquals("Shapefile", source.getString("format"));
        
        ImportContext context = importer.getContext(0);
        assertEquals(((Directory)context.getTasks().get(0).getData()).getFile().getPath(), 
            source.getString("location"));
        
        JSONArray files = source.getJSONArray("files");
        assertEquals(2, files.size());
        
        JSONArray items = task.getJSONArray("items");
        assertEquals(2, items.size());
        
        JSONObject item = items.getJSONObject(0);
        assertEquals("READY", item.getString("state"));
        
        item = items.getJSONObject(1);
        assertEquals("READY", item.getString("state"));
    }
    
    public void testGetImport2() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/1");
        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(1, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals("READY", task.get("state"));

        JSONObject source = task.getJSONObject("source");
        assertEquals("file", source.getString("type"));
        assertEquals("GeoTIFF", source.getString("format"));
        
        ImportContext context = importer.getContext(1);
        assertEquals(((SpatialFile)context.getTasks().get(0).getData()).getFile().getParentFile().getPath(), 
            source.getString("location"));

        assertEquals("EmissiveCampania.tif", source.getString("file"));

        JSONArray items = task.getJSONArray("items");
        assertEquals(1, items.size());
        
        JSONObject item = items.getJSONObject(0);
        assertEquals("READY", item.getString("state"));
    }

    public void testGetImport3() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/imports/2");
        assertNotNull(json.get("import"));
        
        JSONObject imprt = json.optJSONObject("import");
        assertEquals(2, imprt.getInt("id"));
        
        JSONArray tasks = imprt.getJSONArray("tasks");
        assertEquals(1, tasks.size());

        JSONObject task = tasks.getJSONObject(0);
        assertEquals("INCOMPLETE", task.get("state"));

        JSONObject source = task.getJSONObject("source");
        assertEquals("file", source.getString("type"));
        assertEquals("Shapefile", source.getString("format"));
        assertEquals("archsites.shp", source.getString("file"));

        JSONArray items = task.getJSONArray("items");
        assertEquals(1, items.size());
        
        JSONObject item = items.getJSONObject(0);
        assertEquals("NO_CRS", item.getString("state"));
    }

    public void testGetImportDatabase() throws Exception {
        File dir = unpack("h2/cookbook.zip");

        Map params = new HashMap();
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");
        params.put(H2DataStoreFactory.DATABASE.key, new File(dir, "cookbook").getAbsolutePath());
        importer.createContext(new Database(params));

        JSONObject json = (JSONObject) getAsJSON("/rest/imports/3");
        assertNotNull(json.get("import"));

        JSONObject source = json.getJSONObject("import").getJSONArray("tasks").getJSONObject(0)
            .getJSONObject("source");
        assertEquals("database", source.getString("type"));
        assertEquals("H2", source.getString("format"));

        JSONArray tables = source.getJSONArray("tables");
        assertTrue(tables.contains("point"));
        assertTrue(tables.contains("line"));
        assertTrue(tables.contains("polygon"));
    }

    public void testPost() throws Exception {
        
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        int id = lastId();
        assertTrue( resp.getHeader("Location").endsWith( "/imports/"+ id));

        JSONObject json = (JSONObject) json(resp);
        JSONObject imprt = json.getJSONObject("import");

        assertEquals(id, imprt.getInt("id"));
    }

    public void testPostWithTarget() throws Exception {
        createH2DataStore("sf", "skunkworks");

        String json = 
            "{" + 
                "\"import\": { " + 
                    "\"targetWorkspace\": {" +
                       "\"workspace\": {" + 
                           "\"name\": \"sf\"" + 
                       "}" + 
                    "}," +
                    "\"targetStore\": {" +
                        "\"dataStore\": {" + 
                            "\"name\": \"skunkworks\"" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports", json, "application/json");
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        int id = lastId();
        assertTrue( resp.getHeader("Location").endsWith( "/imports/"+ id));

        ImportContext ctx = importer.getContext(id);
        assertNotNull(ctx);
        assertNotNull(ctx.getTargetWorkspace());
        assertEquals("sf", ctx.getTargetWorkspace().getName());
        assertNotNull(ctx.getTargetStore());
        assertEquals("skunkworks", ctx.getTargetStore().getName());
    }

    private MockHttpServletResponse postAsServletResponseNoContentType(String path, String body) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setBodyContent(body);
        return dispatch(request);
    }

    public void testPostNoMediaType() throws Exception {
        MockHttpServletResponse resp = postAsServletResponseNoContentType("/rest/imports", "");
        assertEquals(201, resp.getStatusCode());
    }

    public void testImportSessionIdNotInt() throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports/foo", "");
        assertEquals(404, resp.getStatusCode());
    }
}
