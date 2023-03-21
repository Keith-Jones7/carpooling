package match;

import model.Coordinates;
import model.Driver;
import model.Passenger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Batch {
    public long cur_time = 0;       //当前batch的时刻
    public Match matching;
    double gap = 0.1;               //随机生成坐标的地理范围，gap越大坐标范围越大

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
     * @param file_name 读取司机的txt文件名，格式：ID lng lat
     */
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

    /**
     *
     * @param file_name 读取乘客的txt文件名，格式：lng1 lat1 lng2 lat2
     */
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
        Random random = new Random(1);
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
        Random random = new Random(1);
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
    
}
