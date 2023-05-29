package model;

import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import ilog.concert.IloNumVar;

public class Pattern {
    public long cur_time;

    public double aim;
    public double sameAim;
    public double etaAim1;

    public double etaAim2;
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
    public MPVariable var;

    public double reducedCost;

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
    public void setAim(double sameAim, double etaAim1, double etaAim2) {
        this.sameAim = sameAim;
        this.etaAim1 = etaAim1;
        this.etaAim2 = etaAim2;
        if (passenger2 == null) {
            this.aim = Param.calPlatformMoney(passenger1.singleDistance);
        } else {
            Passenger p1 = passenger1;
            Passenger p2 = passenger2;
            if (p1 == null) {
                p1 = driver.queue.getFirst();
            }
            double o1_o2 = Param.touringMap.calSpatialDistance(p1.originCoor, p2.originCoor);
            double o2_d1 = Param.touringMap.calSpatialDistance(p2.originCoor, p1.destCoor);
            double o2_d2 = Param.touringMap.calSpatialDistance(p2.originCoor, p2.destCoor);
            double d1_d2 = Param.touringMap.calSpatialDistance(p1.destCoor, p2.destCoor);
            double totalDistance = o1_o2 + Math.min(o2_d1, o2_d2) + d1_d2;
            this.aim = Param.calPlatformMoney(p1.singleDistance, p2.singleDistance, totalDistance);
        }
//        this.aim = (sameAim > 0 ? sameAim + Param.samePlus : 0) + 1 - etaAim1 / Param.MAX_ETA + 1 - etaAim2 / Param.MAX_ETA2;
    }

    public void setCur_time(long cur_time) {
        this.cur_time = cur_time;
    }

    public String toString() {
        return "(" + driverId + ", " + passenger1Id + ", " + passenger2Id + ")" + ": " + aim;
    }
    
    
    public void getPath() {
        if (passenger2 == null) {
            return;
        }
        String path1 = "driving.search(" + driver.curCoor.toString() + "," + passenger2.destCoor.toString() + "," +
                "{waypoints:[" + passenger1.originCoor.toString() + ","
                + passenger2.originCoor.toString() + ","
                + passenger1.destCoor.toString() + "]}";
        String path2 = "driving.search(" + driver.curCoor.toString() + "," + passenger1.destCoor.toString() + "," +
                "{waypoints:[" + passenger1.originCoor.toString() + ","
                + passenger2.originCoor.toString() + ","
                + passenger2.destCoor.toString() + "]}";
        System.out.println(path1);
        System.out.println(path2);

    }
}
