package org.dainst.idaifield.export;

import org.dainst.idaifield.model.GeometryType;
import org.dainst.idaifield.model.Resource;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;


/**
 * @author Thomas Kleinke
 */
class ShapefileWriter {

    private static final String dataSchema =
            "id:String,"
            + "identifier:String,"
            + "shortdesc:String,"
            + "type:String";


    static void writeShapefile(File shapefileFolder,
                                      Map<GeometryType, List<Resource>> resources) throws Exception {

        try {
            for (GeometryType geometryType : resources.keySet()) {
                createFiles(resources.get(geometryType), shapefileFolder, geometryType);
            }
        } catch(Exception e) {
            throw new Exception("Error during shapefile creation", e);
        }
    }


    private static void createFiles(List<Resource> resources, File folder,
                                    GeometryType geometryType) throws Exception {

        String outputFilePath = folder.getAbsolutePath() + File.separator
                + geometryType.name().toLowerCase() + "s.shp";

        File outputFile = new File(outputFilePath);

        MemoryDataStore memoryDataStore = createMemoryDataStore(resources, geometryType);

        if (memoryDataStore != null) writeShapefile(memoryDataStore, outputFile);
    }


    private static MemoryDataStore createMemoryDataStore(List<Resource> resources,
                                                         GeometryType geometryType) throws Exception {

        SimpleFeatureType featureType = createFeatureType(geometryType);

        MemoryDataStore memoryDataStore = new MemoryDataStore();
        memoryDataStore.createSchema(featureType);

        GeometryBuilder geometryBuilder = new GeometryBuilder(JTSFactoryFinder.getGeometryFactory());

        Transaction transaction = Transaction.AUTO_COMMIT;

        boolean dataStoreEmpty = true;

        for (Resource resource : resources) {
            if (addFeature(resource, featureType, geometryType, geometryBuilder, memoryDataStore, transaction)) {
                dataStoreEmpty = false;
            }
        }

        transaction.close();

        return dataStoreEmpty ? null : memoryDataStore;
    }


    private static SimpleFeatureType createFeatureType(GeometryType geometryType) throws Exception {

        String schema = null;

        switch(geometryType) {
            case MULTIPOINT:
                schema = "the_geom:MultiPoint," + dataSchema;
                break;
            case MULTIPOLYLINE:
                schema = "the_geom:MultiLineString," + dataSchema;
                break;
            case MULTIPOLYGON:
                schema = "the_geom:MultiPolygon," + dataSchema;
                break;
        }

        try {
            return DataUtilities.createType(geometryType.name().toLowerCase(), schema);
        } catch (SchemaException e) {
            throw new Exception("Failed to create feature types", e);
        }
    }


    private static boolean addFeature(Resource resource, SimpleFeatureType featureType,
                                      GeometryType geometryType, GeometryBuilder geometryBuilder,
                                      MemoryDataStore memoryDataStore,
                                      Transaction transaction) throws Exception {

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        switch(geometryType) {
            case MULTIPOINT:
                featureBuilder.add(geometryBuilder.buildMultiPointGeometry(resource.getGeometry()));
                break;
            case MULTIPOLYLINE:
                featureBuilder.add(geometryBuilder.buildMultiPolylineGeometry(resource.getGeometry()));
                break;
            case MULTIPOLYGON:
                MultiPolygon multiPolygon = geometryBuilder.buildMultiPolygonGeometry(resource.getGeometry());
                if (multiPolygon != null) {
                    featureBuilder.add(multiPolygon);
                    break;
                } else {
                    return false;
                }
        }

        fillFeatureFields(resource, featureBuilder);

        SimpleFeature feature = featureBuilder.buildFeature(null);
        memoryDataStore.addFeature(feature);

        try {
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            transaction.close();
            throw new Exception("Could not write feature for resource " + resource.getId()
                    + " and featureType " + featureType.getTypeName(), e);
        }

        return true;
    }


    private static void fillFeatureFields(Resource resource, SimpleFeatureBuilder featureBuilder) {

        featureBuilder.add(resource.getId());
        featureBuilder.add(resource.getIdentifier());

        if (resource.getShortDescription() != null) {
            featureBuilder.add(resource.getShortDescription());
        } else {
            featureBuilder.add("");
        }

        featureBuilder.add(resource.getType());
    }


    private static void writeShapefile(MemoryDataStore memoryDataStore, File outputFile) throws Exception {

        SimpleFeatureSource featureSource = memoryDataStore.getFeatureSource(memoryDataStore.getTypeNames()[0]);
        SimpleFeatureType featureType = featureSource.getSchema();

        Map<String, Serializable> creationParams = new HashMap<>();
        creationParams.put("url", DataUtilities.fileToURL(outputFile));

        ShapefileDataStoreFactory factory = (ShapefileDataStoreFactory) FileDataStoreFinder.getDataStoreFactory("shp");
        ShapefileDataStore dataStore = (ShapefileDataStore) factory.createNewDataStore(creationParams);
        dataStore.setCharset(Charset.forName("UTF-8"));
        dataStore.createSchema(featureType);

        SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(dataStore.getTypeNames()[0]);

        Transaction transaction = new DefaultTransaction();
        try {
            SimpleFeatureCollection collection = featureSource.getFeatures();
            featureStore.addFeatures(collection);
            transaction.commit();
        } catch (IOException e) {
            try {
                transaction.rollback();
                throw new Exception("Failed to commit data to feature store", e);
            } catch (IOException e2) {
                throw new Exception("Failed to commit data to feature store; transaction rollback failed", e2);
            }
        } finally {
            transaction.close();
        }
    }
}
