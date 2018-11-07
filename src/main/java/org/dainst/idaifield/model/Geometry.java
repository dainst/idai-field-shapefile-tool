package org.dainst.idaifield.model;


/**
 * @author Thomas Kleinke
 */
public class Geometry {

    private GeometryType type;
    private double[][][][] coordinates;


    public GeometryType getType() {

        return type;
    }


    public void setType(GeometryType geometryType) {

        this.type = geometryType;
    }


    public double[][][][] getCoordinates() {

        return coordinates;
    }


    public void setCoordinates(double[][][][] coordinates) {

        this.coordinates = coordinates;
    }
}
