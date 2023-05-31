package match;

import common.Param;
import model.Coordinates;
import model.Driver;
import model.Passenger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Batch {
    public long curTime = 0;       //当前batch的时刻
    public Match matching;
    public List<Driver> driverList; //司机列表
    public List<Passenger> passengerList;//乘客列表
    int pIndex = 0;
    Map<Integer, Driver> driverMap; //司机ID存储

    public Batch() {
        driverList = new ArrayList<>();
        passengerList = new ArrayList<>();
        driverMap = new HashMap<>();
    }

    /**
     * @param file_name 读取司机的txt文件名，格式：ID lng lat
     */
    public void updateDrivers(String file_name) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file_name));
            String line;
            int idx = 0;
            while ((line = bufferedReader.readLine()) != null && idx <= Param.MAX_DRIVER_NUM) {
                String[] strs = line.split(" ");
                int ID = Integer.parseInt(strs[0]) - 1;
                double lng = Double.parseDouble(strs[1]);
                double lat = Double.parseDouble(strs[2]);
                if (driverMap.containsKey(ID)) {
                    Driver driver = driverMap.get(ID);
                    driver.renew(lng, lat, curTime);
                } else {
                    Driver driver = new Driver(lng, lat, curTime, ID);
                    driverMap.put(ID, driver);
                    driverList.add(driver);
                }
                idx++;
            }
        } catch (Exception e) {
            System.out.println("读取司机信息错误！");
            e.printStackTrace();
        }
    }

    /**
     * @param file_name 读取乘客的txt文件名，格式：lng1 lat1 lng2 lat2
     */
    public void updatePassenger(String file_name, int sample_index) {
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
                        new Coordinates(lng2, lat2), sample_index, pIndex));
                pIndex++;

            }
        } catch (Exception e) {
            System.out.println("读取乘客信息错误！");
            e.printStackTrace();
        }
    }
}
