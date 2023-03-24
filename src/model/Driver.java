package model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
public class Driver {

    public Deque<Passenger> queue;
    public Coordinates cur_coor;
    long cur_time;
    
    public int ID;
    public Driver(double lng, double lat, long cur_time, int ID) {
        this.cur_coor = new Coordinates(lng, lat);
        queue = new ArrayDeque<>();
        this.cur_time = cur_time;
        this.ID = ID;
    }
    public void renew(double lng, double lat, long cur_time) {
        this.cur_coor = new Coordinates(lng, lat);
        this.cur_time = cur_time;
    }
}
