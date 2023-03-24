package model;

import common.Param;
import ilog.concert.IloNumVar;

public class Pattern {

    public double aim;
    public double sameAim;
    public double etaAim;
    public int driverId;
    public int passenger1Id;
    public int passenger2Id;

    public IloNumVar colVar;

    public Pattern(int driverId, int passenger1Id, int passenger2Id) {
        this.driverId = driverId;
        this.passenger1Id = passenger1Id;
        this.passenger2Id = passenger2Id;
    }

    // Todo: 后续可以在这里修改方案变量的目标系数
    public void setAim(double sameAim, double etaAim) {
        this.sameAim = sameAim;
        this.etaAim = etaAim;
        this.aim = Param.obj1Coef * sameAim - etaAim + Param.MAX_ETA;
//        this.profit = same;
//        this.profit = 1;
    }

    public String toString() {
        return "(" + driverId + ", " + passenger1Id + ", " + passenger2Id + ")";
    }
}
