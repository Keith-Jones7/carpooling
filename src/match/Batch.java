package match;

import common.Param;
import model.Coordinates;
import model.Driver;
import model.Passenger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Batch {
    public long cur_time = 0;       //当前batch的时刻
    public Match matching;
    int p_index = 0;

    public List<Driver> driverList; //司机列表
    public List<Passenger> passengerList;//乘客列表
    Map<Integer, Driver> driverMap; //司机ID存储
    
    public Batch() {
        driverList = new ArrayList<>();
        passengerList = new ArrayList<>();
        driverMap = new HashMap<>();
    }

    /**
     *
     * @param   file_name 读取司机的txt文件名，格式：ID lng lat
     * @return  读取文件成功，返回true，否则返回false
     */
    public boolean updateDrivers(String file_name) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file_name));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split(" ");
                int ID = Integer.parseInt(strs[0]);
                double lng = Double.parseDouble(strs[1]);
                double lat = Double.parseDouble(strs[2]);
                if (driverMap.containsKey(ID)) {
                    Driver driver = driverMap.get(ID);
                    driver.renew(lng, lat, cur_time);
                }else {
                    Driver driver = new Driver(lng, lat, cur_time, ID);
                    driverMap.put(ID, driver);
                    driverList.add(driver);
                }
            }
            return true;
        }catch (Exception e) {
            System.out.println("读取司机信息错误！");
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param   file_name 读取乘客的txt文件名，格式：lng1 lat1 lng2 lat2
     * @return  读取文件成功，返回true，否则返回false
     */
    public boolean updatePassenger(String file_name) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file_name));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split(" ");
                double lng1 = Double.parseDouble(strs[0]);
                double lat1 = Double.parseDouble(strs[1]);
                double lng2 = Double.parseDouble(strs[2]);
                double lat2 = Double.parseDouble(strs[3]);
                passengerList.add(new Passenger(new Coordinates(lng1, lat1),
                        new Coordinates(lng2, lat2), cur_time, p_index++));
            }
            return true;
        }catch (Exception e) {
            System.out.println("读取乘客信息错误！");
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updatePassenger(String file_name, int start_time, int end_time) {// Todo: 增加算例数据，按照实际时间导入
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file_name));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split(" ");
                int call_time = Integer.parseInt(strs[0]);
                if (call_time >= start_time && call_time < end_time) {
                    double lng1 = Double.parseDouble(strs[1]);
                    double lat1 = Double.parseDouble(strs[2]);
                    double lng2 = Double.parseDouble(strs[3]);
                    double lat2 = Double.parseDouble(strs[4]);
                    passengerList.add(new Passenger(new Coordinates(lng1, lat1),
                            new Coordinates(lng2, lat2), call_time, p_index++));
                }else if (call_time >= end_time){
                    break;
                }
            }
            return true;
        }catch (Exception e) {
            System.out.println("读取乘客信息错误！");
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 
     * @param num   随机生成的司机数目
     */
    public void generateDrivers(int num) {
        Random random = new Random(1);
        for (int i = 0; i < num; i++) {
            double lng = random.nextDouble() * Param.GAP + 118.6;
            double lat = random.nextDouble() * Param.GAP + 31.9;
            if (driverMap.containsKey(i)) {
                Driver driver = driverMap.get(i);
                driver.renew(lng, lat, cur_time);
            }else {
                Driver driver = new Driver(lng, lat, cur_time, i);
                driverList.add(driver);
                driverMap.put(i, driver);
            }
        }
    }

    /**
     * 
     * @param num   随机生成的乘客数目
     */
    public void generatePassengers(int num) {
        Random random = new Random(1);
        for (int i = 0; i < num; i++) {
            double lng1 = random.nextDouble() * Param.GAP + 118.6;
            double lat1 = random.nextDouble() * Param.GAP + 31.9;
            double lng2 = random.nextDouble() * Param.GAP + 118.3;
            double lat2 = random.nextDouble() * Param.GAP + 31.4;
            passengerList.add(new Passenger(new Coordinates(lng1, lat1),
                    new Coordinates(lng2, lat2), cur_time, p_index++));
        }
    }
}
