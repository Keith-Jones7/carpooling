package model;

import common.Param;

//乘客
public class Passenger {
    public Coordinates originCoor;    //起点坐标

    public Coordinates curCoor;       //当前坐标
    public Coordinates destCoor;      //终点坐标

    //时间戳，最小单位为秒
    public long submitTime;      //订单提交时间

    //时间戳，最小单位为秒
    public long pastTime;
    public long expectedArriveTime;   //乘客预期到达时间
    public double singleDistance;
    public Driver curDriver;
    public int pre;
    public int next;
    public int ID;

    public Passenger() {

    }

    public Passenger(Coordinates origin, Coordinates dest, long submit, int ID) {
        this.originCoor = origin;
        this.destCoor = dest;
        this.curCoor = new Coordinates(origin.lng, origin.lat);
        this.submitTime = submit;
        this.singleDistance = Param.touringMap.calSpatialDistance(originCoor, destCoor);
        this.expectedArriveTime = (long) (Param.touringMap.calTimeDistance(originCoor, destCoor) * Param.DETOUR_RATIO);
        this.ID = ID;
        this.pre = -1;
        this.next = -1;
    }

    public void renew(long cur_time) {
        if (curDriver != null) {
            curCoor = curDriver.curCoor;
        }
        pastTime = cur_time - submitTime;
    }
}

