package model;

import common.Param;
import ilog.concert.IloNumVar;

public class Pattern {

    public double aim;
    public double sameAim;
    public double etaAim;
    public Driver driver;
    
    public Passenger passenger1;
    
    public Passenger passenger2;
    public int driverId;
    public int passenger1Id;
    public int passenger2Id;

    public IloNumVar colVar;

    public Pattern(Driver driver, Passenger passenger1, Passenger passenger2) {
        this.driver = driver;
        this.passenger1 = passenger1;
        this.passenger2 = passenger2;
        this.driverId = driver.ID;
        this.passenger1Id = passenger1 == null ? -1 : passenger1.ID;
        this.passenger2Id = passenger2 == null ? -1 : passenger2.ID;
    }

    // Todo: 后续可以在这里修改方案变量的目标系数
    public void setAim(double sameAim, double etaAim) {
        this.sameAim = sameAim;
        this.etaAim = etaAim;
        this.aim = (sameAim > 0 ? (sameAim + 2) : 0) + 2 - etaAim/Param.MAX_ETA;
//        this.profit = same;
//        this.profit = 1;
    }

    public String toString() {
        return "(" + driverId + ", " + passenger1Id + ", " + passenger2Id + ")";
    }
}
