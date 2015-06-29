package org.geotools.data.mongodb;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.vividsolutions.jts.geom.Point;

import java.util.Properties;
import java.util.Set;

import org.geotools.test.OnlineTestCase;
import org.opengis.feature.simple.SimpleFeature;

public abstract class MongoTestSupport extends OnlineTestCase {

    protected MongoTestSetup testSetup;
    protected MongoDataStore dataStore;
    
    protected MongoClient client;

    protected MongoTestSupport(MongoTestSetup testSetup) {
        this.testSetup = testSetup;
    }

    @Override
    protected String getFixtureId() {
        return "mongodb";
    }

    @Override
    protected boolean isOnline() throws Exception {
        return doConnect() != null;
    }

    @Override
    protected void connect() throws Exception {
         setUp(doConnect());
    }

    DB doConnect() throws Exception {
        MongoClientURI clientURI = new MongoClientURI(fixture.getProperty(MongoDataStoreFactory.DATASTORE_URI.key));
        client = new MongoClient(clientURI);
        return client.getDB(clientURI.getDatabase());
    }

    protected void setUp(DB db) throws Exception {
    	testSetup.setUp(db);
        dataStore = testSetup.createDataStore(fixture);
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        dataStore.dispose();
        client.close();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put(MongoDataStoreFactory.DATASTORE_URI.key, "mongodb://geotools:geotools@localhost:27017/geotools");
        return fixture;
    }

    protected void assertFeature(SimpleFeature f) {
        int i = (Integer) f.getAttribute("properties.intProperty");
        assertFeature(f, i);
    }
    
    protected void assertFeature(SimpleFeature f, int i) {
        assertNotNull(f.getDefaultGeometry());
        Point p = (Point) f.getDefaultGeometry();

        assertNotNull(f.getAttribute("properties.intProperty"));

        assertEquals((double)i, p.getX(), 0.1);
        assertEquals((double)i, p.getY(), 0.1);

        assertNotNull(f.getAttribute("properties.doubleProperty"));
        assertEquals(i + i*0.1, (Double)f.getAttribute("properties.doubleProperty"), 0.1);

        assertNotNull(f.getAttribute("properties.stringProperty"));
        assertEquals(toString(i), (String)f.getAttribute("properties.stringProperty"));
    }

    protected String toString(int i) {
        return i == 0 ? "zero" : i == 1 ? "one" : i == 2 ? "two" : null;
    }
}
