package org.opengeo.data.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;

public abstract class ImporterTestSupport extends GeoServerTestSupport {

    protected Importer importer;

    @Override
    protected void oneTimeSetUp() throws Exception {
        //need to set hint which allows for lax projection lookups to match
        // random wkt to an epsg code
        Hints.putSystemDefault(Hints.COMPARISON_TOLERANCE, 1e-9);
        super.oneTimeSetUp();
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        importer = (Importer) applicationContext.getBean("importer");
    }

    protected File tmpDir() throws Exception {
        File dir = File.createTempFile("importer", "data", new File("target"));
        dir.delete();
        dir.mkdirs();
        return dir;
    }

    protected File unpack(String path) throws Exception {
        return unpack(path, tmpDir());
    }

    protected File getTestDataFile(String path) throws Exception {
        URL url = ImporterTestSupport.class.getResource("../test-data/" + path);
        return new File(url.toURI().getPath());
    }

    protected File unpack(String path, File dir) throws Exception {
        
        File file = file(path, dir);
        
        new VFSWorker().extractTo(file, dir);
        file.delete();
        
        return dir;
    }

    protected File file(String path) throws Exception {
        return file(path, tmpDir());
    }

    protected File file(String path, File dir) throws IOException {
        String filename = new File(path).getName();
        InputStream in = ImporterTestSupport.class.getResourceAsStream("../test-data/" + path);
        
        File file = new File(dir, filename);
        
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(in, out);
        in.close();
        out.flush();
        out.close();

        return file;
    }

    protected void runChecks(String layerName) throws Exception {
        LayerInfo layer = getCatalog().getLayerByName(layerName);
        assertNotNull(layer);
        assertNotNull(layer.getDefaultStyle());
        
        if (layer.getType() == LayerInfo.Type.VECTOR) {
            FeatureTypeInfo featureType = (FeatureTypeInfo) layer.getResource();
            FeatureSource source = featureType.getFeatureSource(null, null);
            assertTrue(source.getCount(Query.ALL) > 0);
            
            //do a wfs request
            Document dom = getAsDOM("wfs?request=getFeature&typename=" + featureType.getPrefixedName());
            assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
            assertEquals(
                source.getCount(Query.ALL), dom.getElementsByTagName(featureType.getPrefixedName()).getLength());
        }

        //do a wms request
        MockHttpServletResponse response = 
            getAsServletResponse("wms/reflect?layers=" + layer.getResource().getPrefixedName());
        assertEquals("image/png", response.getContentType());
    }

    protected DataStoreInfo createH2DataStore(String wsName, String dsName) {
        //create a datastore to import into
        Catalog cat = getCatalog();

        WorkspaceInfo ws = wsName != null ? cat.getWorkspaceByName(wsName) : cat.getDefaultWorkspace();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName(dsName);
        ds.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath()+"/" + dsName);
        params.put("dbtype", "h2");
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);
        
        return ds;
    }

    protected int lastId() {
        Iterator<ImportContext> ctx = importer.getAllContexts();
        int id = -1;
        while (ctx.hasNext()) {
            ctx.next();
            id++;
        }
        return id;
    }

    public static class JSONObjectBuilder extends JSONBuilder {

        public JSONObjectBuilder() {
            super(new StringWriter());
        }
        
        public JSONObject buildObject() {
            return JSONObject.fromObject( ((StringWriter) writer).toString() );
        }
        
    }
}
