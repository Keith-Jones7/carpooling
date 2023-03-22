import model.Driver;
import model.Passenger;
import match.Batch;
import match.Match;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{
        runSample2(4);
    }
    public static void runDefault(int batch_num, int driver_num, int passenger_num) throws Exception{
        Batch batch = new Batch();
        batch.driverList = new ArrayList<>();
        batch.passengerList = new ArrayList<>();
        for (int i = 0; i < batch_num; i++) {
            long start_time = System.currentTimeMillis();
            List<Driver> driverList = batch.generateDrivers(driver_num);
            List<Passenger> passengerList = batch.generatePassengers(passenger_num);
            batch.driverList.addAll(driverList);
            batch.passengerList.addAll(passengerList);
            batch.matching = new Match(batch.driverList, batch.passengerList);
            int result = batch.matching.match(batch.cur_time);
            batch.cur_time += 30;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，求解总消耗时长%d毫秒",
                    i, batch.driverList.size(), batch.passengerList.size(), result, end_time - start_time);
            System.out.println();
        }
    }
    public static void runSample(int batch_num, int sample_index) throws Exception{
        Batch batch = new Batch();
        int time_intervals = 1;
        for (int i = 0; i < batch_num; i++) {
            String file_name_driver = "src/sample/drs" + sample_index + "/d/driver_" + i + ".txt";
            String file_name_passenger = "src/sample/drs" + sample_index + "/p/passenger_" + i + ".txt";
            long start_time = System.currentTimeMillis();
            batch.updateDrivers(file_name_driver);
            batch.updatePassenger(file_name_passenger);
            batch.matching = new Match(batch.driverList, batch.passengerList);
            batch.cur_time += 30;
            int result = batch.matching.match(batch.cur_time);
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，求解总消耗时长%d毫秒",
                    i, batch.driverList.size(), batch.passengerList.size(), result, end_time - start_time);
            System.out.println();
        }
        System.out.println();
    }
    public static void runSample2(int batch_num, int sample_index) throws Exception{
        Batch batch = new Batch();
        for (int i = 0; i < batch_num; i++) {
            //    batch.cur_time = cur_time;
            String file_name_driver = "src/sample/drs" + sample_index + "/d/driver_" + i + ".txt";
            String file_name_passenger = "src/sample/drs" + sample_index + "/p/passenger_" + i + ".txt";
            long start_time = System.currentTimeMillis();
            //  batch.updateDrivers(file_name_driver);
            batch.updatePassenger(file_name_passenger);
            List<Driver> driverList = batch.generateDrivers(2);
            batch.driverList.addAll(driverList);
            int driver_num = batch.driverList.size();
            int passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            System.out.println(batch.matching.passengerList.size());
            batch.cur_time += 30;
            int result = batch.matching.match(batch.cur_time);
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，求解总消耗时长%d毫秒",
                    i, driver_num, passenger_num, result, end_time - start_time);
            System.out.println();
        }
        System.out.println();
    }
}
