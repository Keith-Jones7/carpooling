package match;

import model.Coordinates;
import model.Driver;
import model.Passenger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Batch {
    public long cur_time = 0;
    public Match matching;
    double gap = 0.1;

    public List<Driver> driverList;
    public List<Passenger> passengerList;
    Map<Integer, Driver> driverMap;
    
    public Batch() {
        driverList = new ArrayList<>();
        passengerList = new ArrayList<>();
        driverMap = new HashMap<>();
    }
    public void updateDrivers(String file_name) {
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
                    Driver driver = new Driver(lng, lat, cur_time);
                    driverList.add(driver);
                }
            }
        }catch (Exception e) {
            System.out.println();
        }
    }
    
    public void updatePassenger(String file_name) {
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
                        new Coordinates(lng2, lat2), cur_time));
            }
        }catch (Exception e) {
            System.out.println();
        }
    }
    /**
     * 
     * @param num   随机生成的司机数目
     * @return      返回随机生成的司机
     */
    public List<Driver> generateDrivers(int num) {
        Random random = new Random(7);
        List<Driver> driverList = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            double lng = random.nextDouble() * gap + 118.6;
            double lat = random.nextDouble() * gap + 31.9;
            driverList.add(new Driver(lng, lat, cur_time));
        }
        return driverList;
    }

    /**
     * 
     * @param num   随机生成的乘客数目
     * @return      返回随机生成的乘客
     */
    public List<Passenger> generatePassengers(int num) {
        Random random = new Random(88);
        List<Passenger> passengerList = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            double lng1 = random.nextDouble() * gap + 118.6;
            double lat1 = random.nextDouble() * gap + 31.91;
            double lng2 = random.nextDouble() * gap + 118.6;
            double lat2 = random.nextDouble() * gap + 31.91;
            passengerList.add(new Passenger(new Coordinates(lng1, lat1),
                    new Coordinates(lng2, lat2), cur_time));
        }
        return passengerList;
    }

    /**
     * 
     * @param file_name 读取司机的txt文件名，格式：lng lat
     * @return          返回读取的司机
     */
    public List<Driver> generateDrivers(String file_name) {
        List<Driver> driverList = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file_name));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split(" ");
                double lng = Double.parseDouble(strs[0]);
                double lat = Double.parseDouble(strs[1]);
                driverList.add(new Driver(lng, lat, cur_time));
            }
        }catch (Exception e) {
            System.out.println("获取司机信息错误！");
        }
        return driverList;
    }

    /**
     * 
     * @param file_name 读取乘客的txt文件名，格式：lng1 lat1 lng2 lat2
     * @return          返回读取的乘客
     */
    public List<Passenger> generatePassengers(String file_name) {
        List<Passenger> passengerList = new ArrayList<>();
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
                        new Coordinates(lng2, lat2), cur_time));
            }
        }catch (Exception e) {
            System.out.println("读取乘客信息错误！");
            e.printStackTrace();
        }
        return passengerList;
    }
}
