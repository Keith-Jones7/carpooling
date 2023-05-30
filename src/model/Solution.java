package model;

import common.Param;

import java.io.*;
import java.util.ArrayList;

public class Solution {
    public double profit;
    public int leaveCount;
    public ArrayList<Pattern> patterns;

    public Solution() {
        this.patterns = new ArrayList<>();
        leaveCount = 0;
    }

    public Solution(ArrayList<Pattern> patterns, double profit) {
        this.patterns = patterns;
        this.profit = profit;
    }

    public void outputSolution() {
        for (Pattern pattern : patterns) {
            pattern.getPath();
        }
    }

    public double getAvgEta() {
        double sum = 0;
        int cnt = 0;
        for (Pattern pattern : patterns) {
            Driver driver = pattern.driver;
            Passenger passenger1 = driver.queue.getFirst();
            Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
            sum += Param.touringMap.calTimeDistance(driver.matchCoor, passenger1.originCoor);
            cnt++;
            if (passenger2 != null) {
                sum += Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                cnt++;
            }
        }
        return sum / cnt;
    }

    public void writeToCsv() {
        File etaFile = new File("eta.csv");

        try {
            FileWriter fileWriter = new FileWriter(etaFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Write data
            for (Pattern pattern : patterns) {
                Driver driver = pattern.driver;
                Passenger passenger1 = driver.queue.getFirst();
                Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                double eta1 = Param.touringMap.calTimeDistance(driver.matchCoor, passenger1.originCoor);
                bufferedWriter.append(String.valueOf(eta1));
                if (passenger2 != null) {
                    double eta2 = eta1 + Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                    bufferedWriter.append(String.valueOf(eta2));
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public double getAvgSame() {
        double sum = 0;
        int cnt = 0;
        for (Pattern pattern : patterns) {
            if (pattern.driver.queue.size() == 2) {
                Driver driver = pattern.driver;
                Passenger passenger1 = driver.queue.getFirst();
                Passenger passenger2 = driver.queue.getLast();
                sum += Param.touringMap.calSimilarity(passenger1, passenger2);
                cnt++;
            }
        }
        return sum / cnt;
    }

    public void checkSolution() {
        for (Pattern pattern : this.patterns) {
            Driver driver = pattern.driver;
            if (driver.queue.size() == 1) {
                Passenger p1 = driver.queue.getFirst();
                if (Param.touringMap.calTimeDistance(driver.matchCoor, p1.originCoor) > Param.MAX_ETA) {
                    System.out.println("Error1");
                }
            } else if (driver.queue.size() == 2) {
                Passenger p1 = driver.queue.getFirst();
                Passenger p2 = driver.queue.getLast();
                if (Param.touringMap.calTimeDistance(driver.matchCoor, p1.originCoor) > Param.MAX_ETA) {
                    System.out.println("Error2");
                }
                if (Param.touringMap.calTimeDistance(p1.originCoor, p2.originCoor) > Param.MAX_ETA2) {
                    System.out.println("Error3");
                }
                if (!Param.touringMap.inEllipsoid(p1, p2) && !Param.touringMap.allInEllipsoid(p1, p2)) {
                    System.out.println("Error4");
                }
                if (Param.touringMap.calSimilarity(p1, p2) < Param.MIN_TOURING_SIMILARITY) {
                    System.out.println("Error5");
                }
            } else {
                System.out.println("Error6");
            }
        }
    }

    public void checkSolutionGIS() {
        ArrayList<Pattern> removeList = new ArrayList<>();
        for (Pattern pattern : this.patterns) {
            Driver driver = pattern.driver;
            if (driver.queue.size() == 1) {
                Passenger p1 = driver.queue.getFirst();
                if (Param.gisMap.calTimeDistance(driver.matchCoor, p1.originCoor) > Param.MAX_ETA) {
                    removeList.add(pattern);
                }
            } else if (driver.queue.size() == 2) {
                Passenger p1 = driver.queue.getFirst();
                Passenger p2 = driver.queue.getLast();
                if (Param.gisMap.calTimeDistance(driver.matchCoor, p1.originCoor) > Param.MAX_ETA) {
                    removeList.add(pattern);
                } else if (Param.gisMap.calTimeDistance(p1.originCoor, p2.originCoor) > Param.MAX_ETA2) {
                    removeList.add(pattern);
                } else if (!Param.gisMap.inEllipsoid(p1, p2) && !Param.gisMap.allInEllipsoid(p1, p2)) {
                    removeList.add(pattern);
                } else if (Param.gisMap.calSimilarity(p1, p2) < Param.MIN_TOURING_SIMILARITY) {
                    removeList.add(pattern);
                }
            }
        }
        System.out.println(removeList.size());
        patterns.removeAll(removeList);
    }

    public void outputSolution(int sample_index) {
        String file_name = String.format("test/output/drs%d/solution.csv", sample_index);
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
                double driverLng = pattern.driver.matchCoor.lng;
                double driverLat = pattern.driver.matchCoor.lat;
                Passenger passenger1 = pattern.driver.queue.getFirst();
                Passenger passenger2 = pattern.driver.queue.getLast();
                int passenger1ID = passenger1.ID;
                double passenger1Lng1 = passenger1.originCoor.lng;
                double passenger1Lat1 = passenger1.originCoor.lat;
                double passenger1Lng2 = passenger1.destCoor.lng;
                double passenger1Lat2 = passenger1.destCoor.lat;

                int passenger2ID = passenger2.ID;
                double passenger2Lng1 = passenger2.originCoor.lng;
                double passenger2Lat1 = passenger2.originCoor.lat;
                double passenger2Lng2 = passenger2.destCoor.lng;
                double passenger2Lat2 = passenger2.destCoor.lat;
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
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("导出文件错误！");
        }
    }
}
