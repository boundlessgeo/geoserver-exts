package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import java.io.IOException;
import org.geotools.data.simple.SimpleFeatureWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MongoFeatureWriter implements SimpleFeatureWriter {

    private final DBCollection collection;
    private final SimpleFeatureType featureType;

    private final CollectionMapper mapper;
    private MongoDBObjectFeature current;

    public MongoFeatureWriter(DBCollection collection, SimpleFeatureType featureType, 
        MongoFeatureStore featureStore) {
        this.collection = collection;
        this.featureType = featureType;
        mapper = featureStore.getMapper();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }
    
    @Override
    public boolean hasNext() throws IOException {
        return false;
    }

    @Override
    public SimpleFeature next() throws IOException {
        return current = new MongoDBObjectFeature(new BasicDBObject(), featureType, mapper);
    }
    
    @Override
    public void write() throws IOException {
        if (current == null) {
            throw new IllegalStateException("No current feature, must call next() before write()");
        }
        collection.save(current.getObject());
    }

    @Override
    public void remove() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void close() throws IOException {
        collection.createIndex(new BasicDBObject(mapper.getGeometryPath(), "2dsphere"));
    }

}
