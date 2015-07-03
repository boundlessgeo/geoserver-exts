package org.geotools.data.mongodb.integration;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.vividsolutions.jts.geom.Geometry;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.geotools.TestData;
import org.geotools.data.mongodb.MongoGeometryBuilder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;


/**
 *
 * @author tkunicki@boundlessgeo.com
 */

public class MongoTestUtil {

  static final int PORT;
  
  static {
    String portAsString = System.getProperty("embedmongo.port");
    int port = 27017;
    if (!(portAsString == null || portAsString.isEmpty())) {
      try {
        port = Integer.parseInt(portAsString);
      } catch (NumberFormatException e) {
        System.out.println("Exception extracting EmbedMongo port from property");
      }
    }
    PORT = port;
    System.out.println("EmbedMongo Port is " + PORT);
  }
  
  
  public void testConnect() throws UnknownHostException {
    MongoClient mc = new MongoClient("localhost", PORT);
    try {
      assertThat(mc, is(notNullValue()));
      DB db = mc.getDB("db");
      DBCollection coll = db.getCollection("dbc");
      BasicDBObject bdo = new BasicDBObject("name", "MongoDB").
                              append("type", "database").
                              append("count", 1).
                              append("info", new BasicDBObject("x", 203).append("y", 102));

      coll.insert(bdo);
      System.out.println(coll.findOne());
    } finally {
      mc.close();
    }
  }
  
  @Test
  public void testLoad() throws IOException {
    MongoClient mc = new MongoClient("localhost", PORT);
    try {
      DBCollection dbc = grabDBCollection(mc, "db", "dbc", true);
      ShapefileDataStore sds = loadShapefile("shapes/statepop.shp");
      loadFeatures(dbc, sds.getFeatureSource().getFeatures());
    } finally {
      mc.close();
    }
  }
  
  public void loadFeatures(DBCollection coll, FeatureCollection<?,?> collection) {
    MongoGeometryBuilder gBuilder = new MongoGeometryBuilder();
    
    FeatureIterator<?> iterator = collection.features();
    while (iterator.hasNext()) {
      Feature f = iterator.next();
      Set<Property> pSet = new LinkedHashSet<Property>(f.getProperties());
      BasicDBObjectBuilder bdoBuilder = BasicDBObjectBuilder.start();
      
      GeometryAttribute gAttr = f.getDefaultGeometryProperty();
      bdoBuilder.add("type", "feature");
      bdoBuilder.add("geometry", gBuilder.toObject((Geometry)gAttr.getValue()));
      boolean removed = pSet.remove(gAttr);
      
      bdoBuilder.push("properties");
      for (Property p : pSet) {
        if (p instanceof GeometryAttribute) {
          // why isn't this removed above?
        } else {
          bdoBuilder.add(p.getName().getLocalPart(), p.getValue());
        }
      }
      bdoBuilder.pop();
      coll.insert(bdoBuilder.get());
      coll.createIndex(new BasicDBObject("geometry", "2dsphere"));
    }
  }
  
  
  
  private ShapefileDataStore loadShapefile(String path) throws FileNotFoundException {
    URL url = TestData.url(path);
    assertThat(url, is(notNullValue()));
    ShapefileDataStore store =  new ShapefileDataStore(url);
    assertThat(store, is(notNullValue()));
    return store;
  }
  

  
  private DBCollection grabDBCollection(MongoClient client, String dbName, String dbcName, boolean init) throws UnknownHostException {
      DB db = client.getDB(dbName);
      DBCollection dbc = db.getCollection(dbcName);
      if (init) {
        dbc.dropIndexes();
        dbc.drop();
      }
      return dbc;
   }

}
