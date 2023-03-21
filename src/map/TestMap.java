package map;

import model.Coordinates;
import model.Passenger;

public class TestMap implements TouringMap<Coordinates>, TravelCal<Passenger>  {

    /**
     * SPEED：车辆平均行驶速度,单位千米每秒
     */
    private static final double SPEED = 36 / 3600.0;

    /**
     * EARTH_RADIUS：地球半径
     */
    private static final double EARTH_RADIUS = 6378.137;

    /**
     * 将角度值转为弧度值
     * @param d 角度
     * @return  返回弧度
     */
    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 计算坐标是否重合
     * @param o1 元素1
     * @param o2 元素2
     * @return true: 重合, false: 不重合
     */

    @Override
    public boolean equals(Coordinates o1, Coordinates o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.lat == o2.lat && o1.lng == o2.lng;
    }

    /**
     * 计算两个坐标的空间距离
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离
     */
    @Override
    public double calSpatialDistance(Coordinates o1, Coordinates o2) {
        double radLatGap = rad(o1.lat) - rad(o2.lat);
        double radLngGap = rad(o1.lng) - rad(o2.lng);
        double dis = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(radLatGap / 2), 2)) +
                Math.cos(o1.lat) * Math.cos(o2.lat) * Math.pow(Math.sin(radLngGap / 2), 2));
        dis *= EARTH_RADIUS;
        return dis;
    }

    /**
     *
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离基础上的平均行驶时间
     */
    @Override
    public double calTimeDistance(Coordinates o1, Coordinates o2) {
        double dis = calSpatialDistance(o1, o2);
        return dis / SPEED;
    }

    /**
     * 计算两个乘客的椭圆焦点是否相互包含
     * @param p1    乘客1
     * @param p2    乘客2
     * @return  乘客1的终点在乘客2的椭圆内，且乘客2的起点在乘客1的椭圆内
     */

    @Override
    public boolean inEllipsoid(Passenger p1, Passenger p2) {
        double o1_o2 = calTimeDistance(p1.cur_coor, p2.origin_coor);
        double o2_d1 = calTimeDistance(p2.origin_coor, p1.dest_coor);
        double d1_d2 = calTimeDistance(p1.dest_coor, p2.dest_coor);
        return o1_o2 + o2_d1 + p1.past_time < p1.expected_arrive_time - p1.submit_time
                && o2_d1 + d1_d2 < p2.expected_arrive_time - p2.submit_time;
    }

    /**
     * 计算乘客2是否被乘客1的椭圆完全包含
     * @param p1    乘客1
     * @param p2    乘客2
     * @return  乘客2的两个焦点全部在乘客1的椭圆内
     */
    public boolean allInEllipsoid(Passenger p1, Passenger p2) {
        double o1_o2 = calTimeDistance(p1.cur_coor, p2.origin_coor);
        double o2_d2 = calTimeDistance(p2.origin_coor, p2.dest_coor);
        double d2_d1 = calTimeDistance(p2.dest_coor, p1.dest_coor);
        return o1_o2 + o2_d2 + d2_d1 + p1.past_time < p1.expected_arrive_time - p1.submit_time;

    }

    /**
     * 计算行程相似度
     * @param p1    乘客1
     * @param p2    乘客2
     * @return 返回相同里程与总里程之比
     */
    @Override
    public double calSimilarity(Passenger p1, Passenger p2) {
        if(equals(p1.origin_coor, p2.origin_coor) && equals(p1.dest_coor, p2.dest_coor)) {
            return 1;
        }
        double o1_o2 = calSpatialDistance(p1.origin_coor, p2.origin_coor);
        double o2_d1 = calSpatialDistance(p2.origin_coor, p1.dest_coor);
        double o2_d2 = calTimeDistance(p2.origin_coor, p2.dest_coor);
        double d1_d2 = calTimeDistance(p1.dest_coor, p2.dest_coor);
          
        double same = Math.min(o2_d1, o2_d2);
        return same / (o1_o2 + same + d1_d2);
    }
}
