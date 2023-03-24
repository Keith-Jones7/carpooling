package model;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
public class Solution {
    public double profit;
    public ArrayList<Pattern> patterns;

    public Solution() {
        this.patterns = new ArrayList<>();
    }
    public Solution(ArrayList<Pattern> patterns, double profit) {
        this.patterns = patterns;
        this.profit = profit;
    }
    public void outputSolution(long cur_time) {
        String file_name = String.format("src/output/drs4/solution_%d.csv", cur_time);
        File outputFile = new File(file_name);
        patterns.sort(Comparator.comparingInt(o -> o.driverId));
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            writer.append("driverID,driverLat,driverLng," +
                    "passenger1ID,passenger1Lng1,passenger1Lat1,passenger1Lng2,passenger1Lat2," +
                    "passenger2ID,passenger2Lng1,passenger2Lat1,passenger2Lng2,passenger2Lat2\n");
            for (Pattern pattern : patterns) {
                if (pattern.passenger2Id == -1) {
                    continue;
                }
                int driverID = pattern.driverId;
                if (driverID == 572) {
                    int i = 1;
                }
                double driverLng = pattern.driver.cur_coor.lng;
                double driverLat = pattern.driver.cur_coor.lat;
                
                int passenger1ID = pattern.passenger1Id;
                double passenger1Lng1 = pattern.passenger1.origin_coor.lng;
                double passenger1Lat1 = pattern.passenger1.origin_coor.lat;
                double passenger1Lng2 = pattern.passenger1.dest_coor.lng;
                double passenger1Lat2 = pattern.passenger1.dest_coor.lat;
                
                int passenger2ID = pattern.passenger2Id;
                double passenger2Lng1 = pattern.passenger2.origin_coor.lng;
                double passenger2Lat1 = pattern.passenger2.origin_coor.lat;
                double passenger2Lng2 = pattern.passenger2.dest_coor.lng;
                double passenger2Lat2 = pattern.passenger2.dest_coor.lat;
                writer.append(String.valueOf(driverID)).append(",").
                        append(String.valueOf(driverLng)).append(",").
                        append(String.valueOf(driverLat)).append(",").
                        append(String.valueOf(passenger1ID)).append(",").
                        append(String.valueOf(passenger1Lng1)).append(",").
                        append(String.valueOf(passenger1Lat1)).append(",").
                        append(String.valueOf(passenger1Lng2)).append(",").
                        append(String.valueOf(passenger1Lat2)).append(",").
                        append(String.valueOf(passenger2ID)).append(",").
                        append(String.valueOf(passenger2Lng1)).append(",").
                        append(String.valueOf(passenger2Lat1)).append(",").
                        append(String.valueOf(passenger2Lng2)).append(",").
                        append(String.valueOf(passenger2Lat2)).append("\n");
            }
            writer.close();
        }catch (Exception e) {
            System.out.println("导出文件错误！");
        }
    }
}
