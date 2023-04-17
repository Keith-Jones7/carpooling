package model;

import java.util.ArrayDeque;
import java.util.Deque;

public class Driver {

    public Deque<Passenger> queue;
    public Coordinates curCoor;
    public Coordinates matchCoor;
    public int ID;
    long curTime;

    public Driver(double lng, double lat, long curTime, int ID) {
        this.curCoor = new Coordinates(lng, lat);
        queue = new ArrayDeque<>();
        this.curTime = curTime;
        this.ID = ID;
    }

    public void renew(double lng, double lat, long curTime) {
        this.curCoor = new Coordinates(lng, lat);
        this.curTime = curTime;
    }

    public void saveMatchCoor() {
        this.matchCoor = curCoor;
    }
}
