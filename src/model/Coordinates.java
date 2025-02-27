package model;

public class Coordinates {

    public double lng;
    public double lat;

    public Coordinates(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public String toString() {
        return String.format("new AMap.LngLat(%.6f,%.6f)", lng, lat);
    }
}

