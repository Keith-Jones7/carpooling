package common;

import map.GISMap;
import map.NetMap;
import map.TestMap;
import map.TouringMap;
import model.Coordinates;
import model.Passenger;

import java.util.BitSet;

public class Param {


    public static int MATCH_ALGO = 0;                                       // 匹配算法选择参数  0: cplex  1: match_zjr  2: match_zkj   3: match_ortools
    public static final long MAX_ETA2 = 200;                                      // 接第二个乘客最大eta
    public static final double LEAVING_COFF = 0.5;                              // 乘客根据预期到达时间决定的取消订单时间系数
    public static final double SPEED = 10;                                         // 车辆平均行驶速度,单位米每秒
    public static final double EPS = 1e-8;
    public static final int SEED = 3;
    public static int MAX_DRIVER_NUM = 2000;
    public static int MATCH_MODEL = 2;                                   // 匹配算法模式参数     0: 0-1匹配    1: 1-1匹配    2: 0-2匹配
    public static long MAX_ETA = 300;                                        // 接第一个乘客最大eta
    public static double LINEAR_RATIO = 1;                              //直线距离排除比率
    public static double DETOUR_RATIO = 1.3;                             // 最大绕行比
    public static double MIN_TOURING_SIMILARITY = 0.5;                      // 最小行程相似度
    public static int samePlus = 2;
    public static int MAX_TIME = 1200;                                    // 订单收集期
    public static int COUNT = 0;
    public static double LNG = 94403.94;                                              // 每经度距离, 单位米
    public static double LAT = 111319.49;                                              // 每纬度距离, 单位米
    public static TouringMap<Coordinates, Passenger> touringMap;
    public static TouringMap<Coordinates, Passenger> gisMap;
    public static TouringMap<Coordinates, Passenger> testMap;
    public static int MAP_CHOOSE;                                       // 地图选择参数     1: GISMap    2: NetMap  default: TestMap
    public static double POOL_RATIO = 0.78;
    public static double L0 = 3000;
    public static double P0 = 10;
    public static double UNIT_PH = (1.9 + 0.55 * 3) * 0.001;
    public static double DRIVER_RATIO = 0.8;
    public static double D0 = P0 * DRIVER_RATIO;
    public static double UNIT_DH = UNIT_PH * DRIVER_RATIO;


    public static void setMapChoose(int val) {
        Param.MAP_CHOOSE = val;
        gisMap = new GISMap();
        testMap = new TestMap();
        switch (Param.MAP_CHOOSE) {
            case 1: {
                touringMap = new GISMap();
                break;
            }
            case 2: {
                touringMap = new NetMap();
                break;
            }
            default: {
                touringMap = new TestMap();
            }
        }
    }
    public static double calPlatformMoney(double distance1, double distance2, double totalDistance) {
        return Param.POOL_RATIO * (calPassengerMoney(distance1) + calPassengerMoney(distance2)) - calDriverMoney(totalDistance);
    }

    public static double calPlatformMoney(double distance) {
        return calPassengerMoney(distance) - calDriverMoney(distance);
    }

    public static double calPassengerMoney(double distance) {
        return (Param.P0 + Math.max(0, distance - Param.L0) * Param.UNIT_PH) * 0.9;
    }

    public static double calDriverMoney(double distance) {
        return (Param.D0 + Math.max(0, distance - Param.L0) * Param.UNIT_DH);
    }
    public static double getTimeCost(long start) {
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

    public static boolean[] convertBitToArray(BitSet bit) {
        boolean[] res = new boolean[bit.size()];
        for (int i = bit.nextSetBit(0); i >= 0; i = bit.nextSetBit(i + 1)) {
            res[i] = true;
        }
        return res;
    }
}
