package model;

import common.Param;
import map.GISMap;
import map.TouringMap;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
public class Solution {
    public double profit;
    
    public int leave_count;
    public ArrayList<Pattern> patterns;
    private static TouringMap<Coordinates, Passenger> map;
    public Solution() {
        this.patterns = new ArrayList<>();
        leave_count = 0;
    }
    public Solution(ArrayList<Pattern> patterns, double profit) {
        this.patterns = patterns;
        this.profit = profit;
    }
    public void checkSolution() {
        for (Pattern pattern : this.patterns) {
            Driver driver = pattern.driver;
            if (driver.queue.size() == 1) {
                Passenger p1 = driver.queue.getFirst();
                if (Param.touringMap.calTimeDistance(driver.cur_coor, p1.origin_coor) > Param.MAX_ETA) {
                    System.out.println("Error1");
                }
            }else if (driver.queue.size() == 2) {
                Passenger p1 = driver.queue.getFirst();
                Passenger p2 = driver.queue.getLast();
                if (Param.touringMap.calTimeDistance(driver.cur_coor, p1.origin_coor) > Param.MAX_ETA) {
                    System.out.println("Error2");
                }
                if (Param.touringMap.calTimeDistance(p1.origin_coor, p2.origin_coor) > Param.MAX_ETA2) {
                    System.out.println("Error3");
                }
                if (!Param.touringMap.inEllipsoid(p1, p2) && !Param.touringMap.allInEllipsoid(p1, p2)) {
                    System.out.println("Error4");
                }
                if (Param.touringMap.calSimilarity(p1, p2) < Param.MIN_TOURING_SIMILARITY) {
                    System.out.println("Error5");
                }
            }else {
                System.out.println("Error6");
            }
        }
    }
    public void outputSolution(int sample_index) {
        String file_name = String.format("test/output/drs%d/solution.csv",sample_index);
        File outputFile = new File(file_name);
        try {
            PrintWriter writer = new PrintWriter(outputFile);
            writer.append("curTime,driverID,driverLat,driverLng," +
                    "passenger1ID,passenger1Lng1,passenger1Lat1,passenger1Lng2,passenger1Lat2," +
                    "passenger2ID,passenger2Lng1,passenger2Lat1,passenger2Lng2,passenger2Lat2\n");
            for (Pattern pattern : patterns) {
                if (pattern.passenger2Id == -1) {
                    continue;
                }
                long curTime = pattern.cur_time;
                int driverID = pattern.driverId;
                double driverLng = pattern.driver.match_coor.lng;
                double driverLat = pattern.driver.match_coor.lat;
                Passenger passenger1 = pattern.driver.queue.getFirst();
                Passenger passenger2 = pattern.driver.queue.getLast();
                int passenger1ID = passenger1.ID;
                double passenger1Lng1 = passenger1.origin_coor.lng;
                double passenger1Lat1 = passenger1.origin_coor.lat;
                double passenger1Lng2 = passenger1.dest_coor.lng;
                double passenger1Lat2 = passenger1.dest_coor.lat;
                
                int passenger2ID = passenger2.ID;
                double passenger2Lng1 = passenger2.origin_coor.lng;
                double passenger2Lat1 = passenger2.origin_coor.lat;
                double passenger2Lng2 = passenger2.dest_coor.lng;
                double passenger2Lat2 = passenger2.dest_coor.lat;
                writer.append(String.valueOf(curTime)).append(",").
                        append(String.valueOf(driverID)).append(",").
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
            e.printStackTrace();
            System.out.println("导出文件错误！");
        }
    }
}
