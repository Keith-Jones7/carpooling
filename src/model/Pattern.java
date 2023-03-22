package model;

import common.Param;
import ilog.concert.IloNumVar;

public class Pattern {

    public double profit;
    public double same;
    public double eta;
    public int driverId;
    public int passenger1Id;
    public int passenger2Id;

    public IloNumVar colVar;

    public Pattern(int driverId, int passenger1Id, int passenger2Id) {
        this.driverId = driverId;
        this.passenger1Id = passenger1Id;
        this.passenger2Id = passenger2Id;
    }

    public Pattern(double same, double eta, int driverId, int passenger1Id, int passenger2Id) {
        this.same = same;
        this.eta = eta;
        this.profit = same - eta;
        this.driverId = driverId;
        this.passenger1Id = passenger1Id;
        this.passenger2Id = passenger2Id;
    }

    // Todo: 后续可以在这里修改方案变量的目标系数
    public void setTime(double same, double eta) {
        this.same = same;
        this.eta = eta;
        this.profit = Param.obj1Coef * same - eta + Param.MAX_ETA;
//        this.profit = same;
    }
}
