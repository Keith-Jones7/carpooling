package model;

public class Coordinates {

    public double lng;
    public double lat;
    public Coordinates(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }
    public String printCoor() {
        return String.format("lng: %.6f, lat: %.6f", lng, lat);
    }
}

