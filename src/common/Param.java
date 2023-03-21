package common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Param {
    public static final double EPS = 1e-6;
    public static final int SEED = 3;
    public static Random RND;
    public static final double eps = 1e-3;

    public static final double obj1Coef = 100;

    public static final double delta = 300;
    public static final double sigma = 300;

    public static void renewRandom() {
        RND = new Random(SEED);
    }

    public static double getTimecost(long start) {
        return 0.001 * (System.currentTimeMillis() - start);
    }

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(new Date());
        // return LocalDate.now().toString();
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
        return sdf.format(new Date());
        // return LocalDateTime.now().toString();
    }

    public static int min(int[] nums) {
        int min = nums[0];
        for (int num : nums)
            min = num < min ? num : min;
        return min;
    }

    public static int max(int[] nums) {
        int max = nums[0];
        for (int num : nums)
            max = num > max ? num : max;
        return max;
    }

    public static boolean isInt(double d) {
        return equals(d, Math.round(d));
    }

    public static boolean areInt(double[] d) {
        for (int i = 0; i < d.length; i++) {
            if (!isInt(d[i])) {
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

    public static int getRandomNum(int n1, int n2) { // [n1, n2)
        return RND.nextInt(n2 - n1) + n1;
    }

    public static void copyTo(int[] src, int[] dest) {
        System.arraycopy(src, 0, dest, 0, src.length);
    }

    public static void copyTo(int[][] src, int[][] dest) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
    }

    public static void copyTo(boolean[][] src, boolean[][] dest) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
    }

    // required: left[0] < right[0], ASC order, no duplicated items
    public static int[] mergeSort(int[] left, int[] right) {
        int[] res = new int[left.length + right.length];
        int h = 0; // index in res
        int i = 0; // index in left
        int j = 0; // index in right
        while (i < left.length && j < right.length) {
            if (left[i] < right[j]) { // <
                res[h++] = left[i++];
            } else { // >
                res[h++] = right[j++];
            }
        }
        while (i < left.length) {
            res[h++] = left[i++];
        }
        while (j < right.length) {
            res[h++] = right[j++];
        }
        return res;
    }
}
