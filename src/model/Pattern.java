package model;

import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import ilog.concert.IloNumVar;
import map.GISMap;

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
        setAim();
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
        this.aim = (sameAim > 0 ? sameAim + 2 : 0) + 1 - etaAim1/Param.MAX_ETA + 1 - etaAim2 / Param.MAX_ETA2;
        //this.aim = (sameAim > 0 ? sameAim + 2 : 0);
    }
    public void setCur_time(long cur_time) {
        this.cur_time = cur_time;
    }
    public void setAim() {
        if (passenger1 == null) {
            Passenger passenger1 = driver.queue.getFirst();
            double eta2 = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
            if (eta2 > Param.MAX_ETA2 || (!Param.touringMap.inEllipsoid(passenger1, passenger2) && !Param.touringMap.allInEllipsoid(passenger1, passenger2))
                    || Param.touringMap.calSimilarity(passenger1, passenger2) == 0) {
                this.aim = 0;
                return;
            }
            double cost = Param.touringMap.calSpatialDistance(driver.cur_coor, passenger1.origin_coor) +
                    Param.touringMap.calSpatialDistance(passenger1.origin_coor, passenger2.origin_coor) +
                    Param.touringMap.calSameTravel(passenger1, passenger2) +
                    Param.touringMap.calSpatialDistance(passenger1.dest_coor, passenger2.dest_coor);
            double income = passenger1.single_distance + passenger2.single_distance;
            this.aim = income * 2 - cost;
        }
        else if (passenger2 == null) {
            double eta1 = Param.touringMap.calTimeDistance(driver.cur_coor, passenger1.origin_coor);
            if (eta1 > Param.MAX_ETA) {
                this.aim = 0;
                return;
            }
            double cost = Param.calSpatialDistance(driver.cur_coor, passenger1.origin_coor) + 
                    Param.touringMap.calSpatialDistance(passenger1.origin_coor, passenger1.dest_coor);
            double income = passenger1.single_distance;
            this.aim = income * 2 - cost;
        }else {
            double eta1 = Param.touringMap.calTimeDistance(driver.cur_coor, passenger1.origin_coor);
            double eta2 = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
            if (eta1 > Param.MAX_ETA || eta2 > Param.MAX_ETA2
                    || (!Param.touringMap.inEllipsoid(passenger1, passenger2) && !Param.touringMap.allInEllipsoid(passenger1, passenger2))
                    || Param.touringMap.calSimilarity(passenger1, passenger2) == 0) {
                this.aim = 0;
                return;
            }
            double cost = Param.touringMap.calSpatialDistance(driver.cur_coor, passenger1.origin_coor) +
                    Param.touringMap.calSpatialDistance(passenger1.origin_coor, passenger2.origin_coor) +
                    Param.touringMap.calSameTravel(passenger1, passenger2) +
                    Param.touringMap.calSpatialDistance(passenger1.dest_coor, passenger2.dest_coor);
            double income = passenger1.single_distance + passenger2.single_distance;
            this.aim = income * 2 - cost;
        }
    }
    public String toString() {
        return "(" + driverId + ", " + passenger1Id + ", " + passenger2Id + ")" + ": " + aim;
    }
}
