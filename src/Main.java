import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import match.Batch;
import match.Match;
import model.Pattern;
import model.Solution;

import java.util.Arrays;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws Exception{
        Param.setMapChoose();
        for (Param.MAX_TIME = 600; Param.MAX_TIME < 601; Param.MAX_TIME += 30) {
            testSpeed(2);
        }

//        test();
//        runDefault(30);
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
                batch.updatePassenger(file_name_passenger, i);
            }
            int size2 = batch.passengerList.size();
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO, Param.MATCH_MODEL);
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
                batch.updatePassenger(file_name_passenger, sample_index);
            }
            int size2 = batch.passengerList.size();
            int waiting_driver_num = batch.driverList.size();
            int waiting_passenger_num = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution cur_solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO, Param.MATCH_MODEL);
//            System.out.println(System.currentTimeMillis() - time);
            solution.profit += cur_solution.profit;
            batch.cur_time += time_interval;
            int result = 0;
            for (Pattern pattern : cur_solution.patterns) {
                if (pattern.passenger2Id != -1) {
                    solution.patterns.add(pattern);
                    result++;
                }
            }
            solution.leave_count += cur_solution.leave_count;
            passenger_sum += size2 - size1;
            match_sum += result * 2;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，取消订单乘客数为%d，求解总消耗时长%d毫秒",
                    end / time_interval, waiting_driver_num, waiting_passenger_num, result,
                    batch.driverList.size(), batch.passengerList.size(), cur_solution.leave_count, end_time - start_time);
            System.out.println();

          }
        //solution.outputSolution(sample_index);
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，取消订单乘客数为%d，拼车成功率为%.2f%%",
                passenger_sum, match_sum, passenger_sum - match_sum - batch.passengerList.size() - solution.leave_count, 
                batch.passengerList.size(), solution.leave_count, (double) match_sum / passenger_sum * 100);
        System.out.println();
        System.out.println(solution.profit);
        System.out.print(Param.timeCostOnGenPatterns);
        return solution;
    }
    public static void testSpeed(int sample_index) throws Exception {
        Batch batch = new Batch();
        long start_time = System.currentTimeMillis();
        int start = 0, end = Param.MAX_TIME;
        String file_name_driver = "test/sample/drs" + sample_index + "/d/drivers_t" + start + ".txt";
        batch.updateDrivers(file_name_driver);
        for (int i = start; i < end; i++) {
            String file_name_passenger = "test/sample/drs" + sample_index + "/p/passengers_t" + i + ".txt";
            batch.updatePassenger(file_name_passenger, sample_index);
        }
        int waiting_driver_num = batch.driverList.size();
        int waiting_passenger_num = batch.passengerList.size();
        batch.matching = new Match(batch.driverList, batch.passengerList);

        Solution solution = batch.matching.match(batch.cur_time, Param.MATCH_ALGO, Param.MATCH_MODEL);
//            System.out.println(System.currentTimeMillis() - time);
        long end_time = System.currentTimeMillis();
        long diff = end_time - start_time;
        System.out.printf("%d\t%d\t\t%.6f\t%d%n", waiting_driver_num, waiting_passenger_num, solution.profit, diff);

    }
    public static void test() {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        int[][] costMatrix = {
                {0, 3, 2, 5},
                {3, 0, 1, 4},
                {2, 1, 0, 7},
                {5, 4, 7, 0}
        };
        int len = costMatrix.length;
        MPVariable[][] variables = new MPVariable[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                variables[i][j] = solver.makeVar(0, 1, false, i + "," + j);
            }
        }
        MPObjective obj = solver.objective();
        obj.setMaximization();
        for (int i = 0; i < len; i++) {
            MPConstraint constraint1 = solver.makeConstraint(0, 1);
            MPConstraint constraint2 = solver.makeConstraint(0 ,1);
            for (int j = 0; j < len; j++) {
                constraint1.setCoefficient(variables[i][j], 1);
                constraint2.setCoefficient(variables[j][i], 1);
                obj.setCoefficient(variables[i][j], costMatrix[i][j]);
            }
        }
        for (int i = 0; i < len; i++) {
            MPConstraint constraint = solver.makeConstraint(0, 0);
            for (int j = i + 1; j < len; j++) {
                constraint.setCoefficient(variables[i][j], 1);
                constraint.setCoefficient(variables[j][i], -1);
            }
        }
        solver.solve();
        double[][] result = new double[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                result[i][j] = variables[i][j].solutionValue();
            }
        }
        System.out.println(Arrays.deepToString(result));
        System.out.println(obj.value());
    }
}
