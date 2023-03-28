package model;

import common.Param;
import ilog.concert.IloNumVar;
import map.GISMap;

public class Pattern {
    public long cur_time;

    public double aim;
    public double sameAim;
    public double etaAim;
    public Driver driver;
    
    public Passenger passenger1;
    
    public Passenger passenger2;
    public int driverId;
    public int passenger1Id;
    public int passenger2Id;
    public int driverIdx;
    public int passenger1Idx;
    public int passenger2Idx;

    public IloNumVar colVar;

    public Pattern(Driver driver, Passenger passenger1, Passenger passenger2) {
        this.driver = driver;
        this.passenger1 = passenger1;
        this.passenger2 = passenger2;
        this.driverId = driver.ID;
        this.passenger1Id = passenger1 == null ? -1 : passenger1.ID;
        this.passenger2Id = passenger2 == null ? -1 : passenger2.ID;
    }

    public void setIdx(int driverIdx, int passenger1Idx, int passenger2Idx) {
        this.driverIdx = driverIdx;
        this.passenger1Idx = passenger1Idx;
        this.passenger2Idx = passenger2Idx;
    }

    // Todo: 后续可以在这里修改方案变量的目标系数
    public void setAim(double sameAim, double etaAim) {
        this.sameAim = sameAim;
        this.etaAim = etaAim;
        this.aim = sameAim + 2 - etaAim/Param.MAX_ETA;
//        this.aim = (sameAim > 0 ? 1 : 0);
    }
    public void setCur_time(long cur_time) {
        this.cur_time = cur_time;
    }
    public String toString() {
        return "(" + driverId + ", " + passenger1Id + ", " + passenger2Id + ")";
    }
}
