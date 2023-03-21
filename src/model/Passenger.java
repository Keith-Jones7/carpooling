package model;

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
    public Driver cur_driver;
    public Passenger() {

    }

    public Passenger(Coordinates origin, Coordinates dest, long submit) {
        this.origin_coor = origin;
        this.dest_coor = dest;
        this.cur_coor = new Coordinates(origin.lng, origin.lat);
        this.submit_time = submit;
        this.expected_arrive_time = (long) new TestMap().calTimeDistance(origin_coor, dest_coor) + 600;
    }
    public void renew(long cur_time) {
        if (cur_driver != null) {
            cur_coor.lng = cur_driver.cur_coor.lng;
            cur_coor.lat = cur_driver.cur_coor.lat;
        }
        past_time = cur_time - submit_time;
    }
}
