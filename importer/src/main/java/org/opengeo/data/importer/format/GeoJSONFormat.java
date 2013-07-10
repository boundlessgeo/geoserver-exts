package org.opengeo.data.importer.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.FeatureReader;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.VectorFormat;
import org.opengeo.data.importer.job.ProgressMonitor;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public class GeoJSONFormat extends VectorFormat {

    static Logger LOG = Logging.getLogger(GeoJSONFormat.class);

    static CoordinateReferenceSystem GEOJSON_CRS;
    static {
        try {
            GEOJSON_CRS = CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public FeatureReader read(ImportData data, ImportItem item) throws IOException {
        final FeatureType featureType = 
            (FeatureType) item.getMetadata().get(FeatureType.class);
        final FeatureIterator it = new FeatureJSON().streamFeatureCollection(file(data, item));

        return new FeatureReader() {

            @Override
            public FeatureType getFeatureType() {
                return featureType;
            }

            @Override
            public boolean hasNext() throws IOException {
                return it.hasNext();
            }

            @Override
            public Feature next() throws IOException, IllegalArgumentException,
                    NoSuchElementException {
                return it.next();
            }

            @Override
            public void close() throws IOException {
                it.close();
            }
        };
    }
    
    @Override
    public void dispose(FeatureReader reader, ImportItem item) throws IOException {
        reader.close();
    }
    
    @Override
    public int getFeatureCount(ImportData data, ImportItem item) throws IOException {
        return -1;
    }
    
    @Override
    public String getName() {
        return "GeoJSON";
    }
    
    @Override
    public boolean canRead(ImportData data) throws IOException {
        Optional<File> file = file(data);
        if (file.isPresent()) {
            return sniff(file.get()) != null;
        }

        return false;
    }

    SimpleFeature sniff(File file) {
        try {
            FeatureIterator it = new FeatureJSON().streamFeatureCollection(file);
            try {
                if (it.hasNext()) {
                    return (SimpleFeature) it.next();
                }
            }
            finally {
                it.close();
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error reading fiel as json", e);
        }
        return null;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog) 
        throws IOException {
        // direct import not supported
        return null;
    }
    
    @Override
    public List<ImportItem> list(ImportData data, Catalog catalog, ProgressMonitor monitor) 
        throws IOException {

        if (data instanceof Directory) {
            List<ImportItem> items = new ArrayList<ImportItem>();
            for (FileData file : ((Directory)data).getFiles()) {
                items.add(item(file, catalog));
            }
            return items;
        }
        else {
            return Arrays.asList(item(data, catalog));
        }
    }

    ImportItem item(ImportData data, Catalog catalog) throws IOException {
        File file = file(data).get();
        
        // grab first feature to check its crs
        SimpleFeature first = sniff(file);
        Preconditions.checkNotNull(first);

        // get the raw feature type, and rename it 
        SimpleFeatureType featureType = first.getFeatureType();

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(featureType);
        tb.setName(FilenameUtils.getBaseName(file.getName()));
        featureType = tb.buildFeatureType();

        // create the feature type
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName(FilenameUtils.getBaseName(file.getName()));
        ft.setNativeName(ft.getName());

        // crs
        CoordinateReferenceSystem crs = GEOJSON_CRS;
        if (featureType != null && featureType.getCoordinateReferenceSystem() != null) {
            crs = featureType.getCoordinateReferenceSystem();
        }
        ft.setNativeCRS(crs);

        String srs = srs(crs);
        if (srs != null) {
            ft.setSRS(srs);
        }

        // bounds
        ReferencedEnvelope bounds = new ReferencedEnvelope(crs);

        FeatureIterator<SimpleFeature> it = new FeatureJSON().streamFeatureCollection(file);
        while(it.hasNext()) {
            SimpleFeature f = it.next();
            bounds.include(f.getBounds());
        }
        ft.setNativeBoundingBox(bounds);

        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(ft);

        ImportItem item = new ImportItem(layer);
        item.getMetadata().put(FeatureType.class, featureType);

        return item;
    }

    File file(ImportData data, final ImportItem item) {
        if (data instanceof Directory) {
            return Iterables.find(((Directory) data).getFiles(), new Predicate<FileData>() {
                @Override
                public boolean apply(FileData input) {
                    return FilenameUtils.getBaseName(input.getFile().getName())
                        .equals(item.getLayer().getName());
                }
            }).getFile();
        }
        else {
            return file(data).get();
        }
    }
    Optional<File> file(ImportData data) {
        if (data instanceof FileData) {
            return Optional.of(((FileData) data).getFile());
        }
        return Optional.absent();
    }

    String srs(CoordinateReferenceSystem crs) {
        Integer epsg = null;
        
        try {
            epsg = CRS.lookupEpsgCode(crs, false);
            if (epsg == null) {
                epsg = CRS.lookupEpsgCode(crs, true);
            }
        }
        catch(Exception e) {
            LOG.log(Level.FINER, "Error looking up epsg code", e);
        }
        return epsg != null ? "EPSG:" + epsg : null;
    }
}
