package common;

import map.GISMap;
import map.TestMap;
import map.TouringMap;
import model.Coordinates;
import model.Passenger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Param {

    public static final int MAP_CHOOSE = 1;                                       // 地图选择参数     1: TestMap    2: GISMap
    public static final int MATCH_ALGO = 1;                                       // 匹配算法选择参数  1: match_zjr  2: match_zkj   3: match_ortools
    public static int MATCH_MODEL = 2;                                   // 匹配算法模式参数     0: 0-1匹配    1: 1-1匹配    2: 0-2匹配
    public static boolean LP_IP = false;                                             // 模型参数     true: LP    false: IP
    public static boolean USE_CG = false;                                               // cg使用参数
    public static long MAX_ETA = 180;                                        // 接第一个乘客最大eta
    public static final long MAX_ETA2 = 120;                                      // 接第二个乘客最大eta
    public static double DETOUR_RATIO = 1.4;                             // 最大绕行比
    public static double MIN_TOURING_SIMILARITY = 0.4;                      // 最小行程相似度
    public static int samePlus = 2;
    public static int MAX_TIME = 1200;                                    // 订单收集期
    public static final double LEAVING_COFF = 1;                              // 乘客根据预期到达时间决定的取消订单时间系数
    public static int COUNT = 0;
    public static int timeCost = 0;
    public static final double SPEED = 10;                                         // 车辆平均行驶速度,单位米每秒

    public static double LNG = 94403.94;                                              // 每经度距离, 单位米
    public static double LAT = 111319.49;                                              // 每纬度距离, 单位米
    public static final double GAP = 0.1;
    public static final double EPS = 1e-8;
    public static final int SEED = 3;
    public static Random RND;
    public static final double eps = 1e-3;

    public static TouringMap<Coordinates, Passenger> touringMap;

    public static double timeCostOnGenPatterns = 0;

    public static void setMapChoose() {
        if (Param.MAP_CHOOSE == 2) {
            touringMap = new GISMap();
        }else {
            touringMap = new TestMap();
        }
    }
    /**
     * 将角度值转为弧度值
     * @param d 角度
     * @return  返回弧度
     */
    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 计算两个坐标的空间距离
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离
     */
    public static double calSpatialDistance(Coordinates o1, Coordinates o2) {
        double lngGap = (o1.lng - o2.lng) * Param.LNG;
        double latGap = (o1.lat - o2.lat) * Param.LAT;
        return Math.sqrt(lngGap * lngGap + latGap * latGap);
    }
    
    /**
     *
     * @param o1 坐标1
     * @param o2 坐标2
     * @return 返回直线距离基础上的平均行驶时间
     */
    public static double calTimeDistance(Coordinates o1, Coordinates o2) {
        double dis = calSpatialDistance(o1, o2);
        return dis / SPEED;
    }

    /**
     * 计算两个乘客的椭圆焦点是否相互包含
     * @param p1    乘客1
     * @param p2    乘客2
     * @return  乘客1的终点在乘客2的椭圆内，且乘客2的起点在乘客1的椭圆内
     */
    
    public static boolean inEllipsoid(Passenger p1, Passenger p2) {
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
    public static boolean allInEllipsoid(Passenger p1, Passenger p2) {
        double o1_o2 = calTimeDistance(p1.cur_coor, p2.origin_coor);
        double o2_d2 = calTimeDistance(p2.origin_coor, p2.dest_coor);
        double d2_d1 = calTimeDistance(p2.dest_coor, p1.dest_coor);
        return o1_o2 + o2_d2 + d2_d1 + p1.past_time < p1.expected_arrive_time - p1.submit_time;
    }

    public static double getTimecost(long start) {
        return 0.001 * (System.currentTimeMillis() - start);
    }

    public static int min(int[] nums) {
        int min = nums[0];
        for (int num : nums)
            min = Math.min(num, min);
        return min;
    }

    public static int max(int[] nums) {
        int max = nums[0];
        for (int num : nums)
            max = Math.max(num, max);
        return max;
    }

    public static boolean isInt(double d) {
        return equals(d, Math.round(d));
    }

    public static boolean areInt(double[] d) {
        for (double v : d) {
            if (!isInt(v)) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double d1, double d2) {
        return Math.abs(d1 - d2) < EPS;
    }

    public static boolean equals(double d, int i) {
        return Math.abs(d - i) < EPS;
    }

    public static int roundToInt(double d) {
        return (int) Math.round(d);
    }

    public static int ceilToInt(double d) {
        return (int) Math.ceil(d - EPS);
    }
}
