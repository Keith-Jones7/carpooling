package model;

import ilog.concert.IloNumVar;

public class Pattern {

    public double time;
    public double sameTime;
    public double getTime;
    public int driverId;
    public int passenger1Id;
    public int passenger2Id;

    public IloNumVar colVar;



    public Pattern(double sameTime, double getTime, int driverId, int passenger1Id, int passenger2Id) {
        this.sameTime = sameTime;
        this.getTime = getTime;
        this.time = sameTime - getTime;
        this.driverId = driverId;
        this.passenger1Id = passenger1Id;
        this.passenger2Id = passenger2Id;
    }
}
