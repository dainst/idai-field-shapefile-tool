package org.dainst.idaifield.export;

import org.dainst.idaifield.model.Geometry;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Thomas Kleinke
 */
class GeometryBuilder {

    private GeometryFactory geometryFactory;


    GeometryBuilder(GeometryFactory geometryFactory) {

        this.geometryFactory = geometryFactory;
    }


    MultiPoint buildMultiPointGeometry(Geometry geometry) {

        double[][] coordinates = geometry.getCoordinates()[0][0];

        Point[] points = new Point[coordinates.length];

        for (int i = 0; i < coordinates.length; i++) {
            points[i] = geometryFactory.createPoint(new Coordinate(coordinates[i][0], coordinates[i][1]));
        }

        return geometryFactory.createMultiPoint(points);
    }


    MultiLineString buildMultiPolylineGeometry(Geometry geometry) {

        double[][][] coordinates = geometry.getCoordinates()[0];

        LineString[] polylines = new LineString[coordinates.length];

        for (int i = 0; i < coordinates.length; i++) {
            double[][] polylineCoordinates = coordinates[i];
            Coordinate[] points = new Coordinate[polylineCoordinates.length];

            for (int j = 0; j < polylineCoordinates.length; j++) {
                double[] pointCoordinates = polylineCoordinates[j];
                points[j] = new Coordinate(pointCoordinates[0], pointCoordinates[1]);
            }

            polylines[i] = geometryFactory.createLineString(points);
        }

        return geometryFactory.createMultiLineString(polylines);
    }


    MultiPolygon buildMultiPolygonGeometry(Geometry geometry) {

        double[][][][] coordinates = geometry.getCoordinates();

        Polygon[] polygons = new Polygon[coordinates.length];

        for (int i = 0; i < coordinates.length; i++) {
            double[][][] polygonCoordinates = coordinates[i];
            LinearRing[] linearRings = new LinearRing[polygonCoordinates.length];

            for (int j = 0; j < polygonCoordinates.length; j++) {
                double[][] linearRingCoordinates = polygonCoordinates[j];
                List<Coordinate> points = new ArrayList<>();

                for (double[] pointCoordinates : linearRingCoordinates) {
                    points.add(new Coordinate(pointCoordinates[0], pointCoordinates[1]));
                }

                closeRingIfNecessary(points);

                Coordinate[] pointsArray = new Coordinate[points.size()];
                linearRings[j] = geometryFactory.createLinearRing(points.toArray(pointsArray));
            }

            if (linearRings.length == 0) return null;

            LinearRing shell = linearRings[0];

            if (!shell.isClosed() || !shell.isValid()) {
                System.err.println("Shell is invalid or not closed");
                return null;
            }

            LinearRing[] holes = null;
            if (linearRings.length > 1) {
                holes = Arrays.copyOfRange(linearRings, 1, linearRings.length);

                for (int j = 0; j < holes.length; j++) {
                    if (!holes[j].isClosed() || !holes[j].isValid()) {
                        System.err.println("Hole " + j + " is invalid or not closed");
                        return null;
                    }
                }
            }

            polygons[i] = geometryFactory.createPolygon(shell, holes);
        }

        return geometryFactory.createMultiPolygon(polygons);
    }


    private static void closeRingIfNecessary(List<Coordinate> points) {

        if (points.get(0).x != points.get(points.size() - 1).x
                || points.get(0).y != points.get(points.size() - 1).y) {
            points.add(new Coordinate(points.get(0).x, points.get(0).y));
        }
    }
}
