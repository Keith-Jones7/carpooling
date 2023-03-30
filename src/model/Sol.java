package model;

import common.Param;
import map.GISMap;
import map.TouringMap;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
public class Sol {
    public double profit;
    
    public int leave_count;
    public ArrayList<Pattern> patterns;
    private static TouringMap<Coordinates, Passenger> map;
    public Sol() {
        this.patterns = new ArrayList<>();
        leave_count = 0;
    }
    public Sol(ArrayList<Pattern> patterns, double profit) {
        this.patterns = patterns;
        this.profit = profit;
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
                if (pattern.driverId == 1344) {
                    int i = 1;
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
    
    public void checkSolution(int checkFlag) {
        map = new GISMap();
        if (checkFlag == 1) {
            ArrayList<Pattern> removePatterns = new ArrayList<>();
            for (Pattern pattern : patterns) {
                if (!checkPattern(pattern)) {
                    removePatterns.add(pattern);
                }
            }
            patterns.removeAll(removePatterns);
        }
    }
    
    public boolean checkPattern(Pattern pattern) {
        Driver driver = pattern.driver;
        Passenger passenger1 = driver.queue.getFirst();
        Passenger passenger2 = pattern.passenger2Id == -1 ? null : driver.queue.getLast();
        double checkEta = map.calTimeDistance(driver.match_coor, passenger1.origin_coor);
        double dist = map.calSpatialDistance(driver.match_coor, passenger1.origin_coor);
        double eta = Param.calSpatialDistance(driver.match_coor, passenger1.origin_coor);
        if (passenger2 == null) {
            return checkEta < Param.MAX_ETA;
        }else {
            return checkEta < Param.MAX_ETA && (map.inEllipsoid(passenger1, passenger2) || map.allInEllipsoid(passenger1, passenger2));
        }
    }
}
