package org.geoserver.importer.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLRawFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    private final InputStream inputStream;

    private final KMLRawReader reader;

    private final SimpleFeatureType featureType;

    public KMLRawFeatureReader(InputStream inputStream, SimpleFeatureType featureType) {
        this.inputStream = inputStream;
        this.featureType = featureType;
        reader = new KMLRawReader(inputStream, KMLRawReader.ReadType.FEATURES,
                featureType);
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public SimpleFeature next() throws IOException, IllegalArgumentException,
            NoSuchElementException {
        return (SimpleFeature) reader.next();
    }

    @Override
    public boolean hasNext() throws IOException {
        return reader.hasNext();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}
