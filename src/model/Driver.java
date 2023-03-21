package model;

import java.util.ArrayDeque;
import java.util.Queue;
public class Driver {

    public Queue<Passenger> queue;
    public Coordinates cur_coor;
    long cur_time;
    public Driver(double lng, double lat, long cur_time) {
        this.cur_coor = new Coordinates(lng, lat);
        queue = new ArrayDeque<>();
        this.cur_time = cur_time;
    }
    public void renew(double lng, double lat, long cur_time) {
        this.cur_coor.lng = lng;
        this.cur_coor.lat = lat;
        this.cur_time = cur_time;
    }
}
