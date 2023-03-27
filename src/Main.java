import common.Param;
import model.Driver;
import model.Passenger;
import match.Batch;
import match.Match;
import model.Pattern;
import model.Solution;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception{
        
        Solution solution = runSample(20, 4);
    }
    public static void runDefault(int batch_num, int driver_num, int passenger_num) throws Exception{
        Solution solution = new Solution();
        Batch batch = new Batch();
        batch.driverList = new ArrayList<>();
        batch.passengerList = new ArrayList<>();
        for (int i = 0; i < batch_num; i++) {
            long start_time = System.currentTimeMillis();
            batch.generateDrivers(driver_num);
            batch.generatePassengers(passenger_num);
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO);
            cur_solution.outputSolution(batch.cur_time);
            int result = 0;
            for (Pattern pattern : cur_solution.patterns) {
                if (pattern.passenger2Id != -1) {
                    solution.patterns.add(pattern);
                    result++;
                }
            }
            batch.cur_time += 30;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，求解总消耗时长%d毫秒",
                    i, waiting_driver_num, waiting_passenger_num, result,
                    batch.driverList.size(), batch.passengerList.size(), end_time - start_time);
            System.out.println();
        }
    }
    public static Solution runSample(int batch_num, int sample_index) throws Exception{
        Batch batch = new Batch();
        Solution solution = new Solution();
        
        int passenger_sum = 0, match_sum = 0;
        for (int i = 0; i < batch_num; i++) {
            String file_name_driver = "src/sample/drs" + sample_index + "/d/drivers_t" + i + ".txt";
            String file_name_passenger = "src/sample/drs" + sample_index + "/p/passengers_t" + i + ".txt";
            long start_time = System.currentTimeMillis();
            if (!batch.updateDrivers(file_name_driver)) {
                batch.generateDrivers(50);
            }
            int size1 = batch.passengerList.size();
            if (!batch.updatePassenger(file_name_passenger)) {
                batch.generatePassengers(100);
            }
            int size2 = batch.passengerList.size();
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            batch.cur_time += 30;
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO);
            int result = 0;
            for (Pattern pattern : cur_solution.patterns) {
                if (pattern.passenger2Id != -1) {
                    solution.patterns.add(pattern);
                    result++;
                }
            }
            passenger_sum += size2 - size1;
            match_sum += result * 2;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，求解总消耗时长%d毫秒",
                    i, waiting_driver_num, waiting_passenger_num, result,
                    batch.driverList.size(), batch.passengerList.size(), end_time - start_time);
            System.out.println();
        }
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，拼车成功率为%.2f%%",
                passenger_sum, match_sum, passenger_sum - match_sum - batch.passengerList.size(), 
                batch.passengerList.size(), (double) match_sum / passenger_sum * 100);
        return solution;
    }
}
