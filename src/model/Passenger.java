package model;

import common.Param;
import map.TestMap;

//乘客
public class Passenger {
    public Coordinates origin_coor;    //起点坐标

    public Coordinates cur_coor;       //当前坐标
    public Coordinates dest_coor;      //终点坐标

    //时间戳，最小单位为秒
    public long submit_time;      //订单提交时间

    //时间戳，最小单位为秒
    public long past_time;
    public long expected_arrive_time;   //乘客预期到达时间
    public double single_distance;
    public Driver cur_driver;
    public int pre;
    public int next;
    public int ID;
    public Passenger() {

    }

    public Passenger(Coordinates origin, Coordinates dest, long submit, int ID) {
        this.origin_coor = origin;
        this.dest_coor = dest;
        this.cur_coor = new Coordinates(origin.lng, origin.lat);
        this.submit_time = submit;
        this.single_distance = Param.touringMap.calSpatialDistance(origin_coor, dest_coor);
        this.expected_arrive_time = (long) (Param.touringMap.calTimeDistance(origin_coor, dest_coor) * Param.DETOUR_RATIO);
        this.ID = ID;
        this.pre = -1;
        this.next = -1;
    }
    public void renew(long cur_time) {
        if (cur_driver != null) {
            cur_coor = cur_driver.cur_coor;
        }
        past_time = cur_time - submit_time;
    }
}

