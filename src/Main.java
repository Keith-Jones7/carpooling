import common.Param;
import match.Batch;
import match.Match;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws Exception{
        
//        Solution solution = runSample(30, 1);
        runDefault(1200);
    }
    public static void runDefault(int time_interval) throws Exception{
        Batch batch = new Batch();
        int passenger_sum = 0, match_sum = 0;
        int start, end = 0;
        while (end < Param.MAX_TIME) {
            long start_time = System.currentTimeMillis();
            start = end;
            end += time_interval;
            String file_name_driver = "test/test/d/drivers_t" + start + ".txt";
            batch.updateDrivers(file_name_driver);
            int size1 = batch.passengerList.size();
            for (int i = start; i < end; i++) {
                String file_name_passenger = "test/test/p/passengers_t" + i + ".txt";
                batch.updatePassenger(file_name_passenger);
            }
            int size2 = batch.passengerList.size();
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO);
            batch.cur_time += time_interval;
            int result = 0;
            for (Pattern pattern : cur_solution.patterns) {
                if (pattern.passenger2Id != -1) {
                    result++;
                }
            }
            passenger_sum += size2 - size1;
            match_sum += result * 2;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，求解总消耗时长%d毫秒",
                    end / time_interval, waiting_driver_num, waiting_passenger_num, result,
                    batch.driverList.size(), batch.passengerList.size(), end_time - start_time);
            System.out.println();
        }
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，拼车成功率为%.2f%%",
                passenger_sum, match_sum, passenger_sum - match_sum - batch.passengerList.size(),
                batch.passengerList.size(), (double) match_sum / passenger_sum * 100);
    }
    public static Solution runSample(int time_interval, int sample_index) throws Exception{
        Batch batch = new Batch();
        Solution solution = new Solution();
        int passenger_sum = 0, match_sum = 0;
        int start, end = 0;
        while (end < Param.MAX_TIME) {
            long start_time = System.currentTimeMillis();
            start = end;
            end += time_interval;
            String file_name_driver = "test/sample/drs" + sample_index + "/d/drivers_t" + start + ".txt";
            batch.updateDrivers(file_name_driver);
            int size1 = batch.passengerList.size();
            for (int i = start; i < end; i++) {
                String file_name_passenger = "test/sample/drs" + sample_index + "/p/passengers_t" + i + ".txt";
                batch.updatePassenger(file_name_passenger);
            }
            int size2 = batch.passengerList.size();
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO);
            batch.cur_time += time_interval;
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
                    end / time_interval, waiting_driver_num, waiting_passenger_num, result,
                    batch.driverList.size(), batch.passengerList.size(), end_time - start_time);
            System.out.println();
        }
        solution.outputSolution(sample_index);
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，拼车成功率为%.2f%%",
                passenger_sum, match_sum, passenger_sum - match_sum - batch.passengerList.size(), 
                batch.passengerList.size(), (double) match_sum / passenger_sum * 100);
        return solution;
    }
}
