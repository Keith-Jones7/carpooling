import algo.BlossomAlgorithm;
import common.Param;
import match.Batch;
import match.Match;
import model.Pattern;
import model.Solution;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        Param.setMapChoose();
        int timeInterval = 5;
        for (; timeInterval < 61; timeInterval += 5) {
            runSample(timeInterval, 2);
        }
    }

    public static void runDefault(int timeInterval) throws Exception {
        Batch batch = new Batch();
        int passengerSum = 0, matchSum = 0;
        int start, end = 0;
        while (end < Param.MAX_TIME) {
            long startTime = System.currentTimeMillis();
            start = end;
            end += timeInterval;
            String fileNameDriver = "test/test/d/drivers_t" + start + ".txt";
            batch.updateDrivers(fileNameDriver);
            int size1 = batch.passengerList.size();
            for (int i = start; i < end; i++) {
                String fileNamePassenger = "test/test/p/passengers_t" + i + ".txt";
                batch.updatePassenger(fileNamePassenger, i);
            }
            int size2 = batch.passengerList.size();
            int waitingDriverNum = batch.driverList.size();
            int waitingPassengerNum = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution curSolution = batch.matching.match(batch.curTime, Param.MATCH_ALGO, Param.MATCH_MODEL);
            batch.curTime += timeInterval;
            int result = 0;
            for (Pattern pattern : curSolution.patterns) {
                if (pattern.passenger2Id != -1) {
                    result++;
                }
            }
            passengerSum += size2 - size1;
            matchSum += result * 2;
            long end_time = System.currentTimeMillis();
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，求解总消耗时长%d毫秒",
                    end / timeInterval, waitingDriverNum, waitingPassengerNum, result,
                    batch.driverList.size(), batch.passengerList.size(), end_time - startTime);
            System.out.println();
        }
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，拼车成功率为%.2f%%",
                passengerSum, matchSum, passengerSum - matchSum - batch.passengerList.size(),
                batch.passengerList.size(), (double) matchSum / passengerSum * 100);

    }

    public static Solution runSample(int timeInterval, int sampleIndex) throws Exception {
        Batch batch = new Batch();
        Solution solution = new Solution();
        int passengerSum = 0, matchSum = 0;
        int start, end = 0;
        while (end < Param.MAX_TIME) {
            long start_time = System.currentTimeMillis();
            start = end;
            end += timeInterval;
            String fileNameDriver = "test/sample/drs" + sampleIndex + "/d/drivers_t" + start + ".txt";
            batch.updateDrivers(fileNameDriver);
            int size1 = batch.passengerList.size();
            for (int i = start; i < end; i++) {
                String fileNamePassenger = "test/sample/drs" + sampleIndex + "/p/passengers_t" + i + ".txt";
                batch.updatePassenger(fileNamePassenger, sampleIndex);
            }
            int size2 = batch.passengerList.size();
            int waitingDriverNum = batch.driverList.size();
            int waitingPassengerNum = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            Solution curSolution = batch.matching.match(batch.curTime, Param.MATCH_ALGO, Param.MATCH_MODEL);
//            System.out.println(System.currentTimeMillis() - time);
            solution.profit += curSolution.profit;
            batch.curTime += timeInterval;
            int result = 0;
            for (Pattern pattern : curSolution.patterns) {
                if (pattern.passenger2Id != -1) {
                    solution.patterns.add(pattern);
                    result++;
                }
            }
            solution.leaveCount += curSolution.leaveCount;
            passengerSum += size2 - size1;
            matchSum += result * 2;
            long end_time = System.currentTimeMillis();
//            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
//                            "当前阶段剩余司机数为%d，剩余乘客数为%d，取消订单乘客数为%d，求解总消耗时长%d毫秒",
//                    end / timeInterval, waitingDriverNum, waitingPassengerNum, result,
//                    batch.driverList.size(), batch.passengerList.size(), curSolution.leaveCount, end_time - start_time);
//            System.out.println();

        }
        //solution.outputSolution(sample_index);
//        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，取消订单乘客数为%d，拼车成功率为%.2f%%",
//                passengerSum, matchSum, passengerSum - matchSum - batch.passengerList.size() - solution.leaveCount,
//                batch.passengerList.size(), solution.leaveCount, (double) matchSum / passengerSum * 100);
//        System.out.println();
        System.out.printf("%.2f  \t%d  \t  %d  \t  %d  \t%.2f   \t%.2f%n", solution.profit, matchSum, passengerSum - matchSum - batch.passengerList.size(), 
                batch.passengerList.size(), solution.getAvgEta(), solution.getAvgSame());
        return solution;
    }

    public static void testSpeed(int sampleIndex) throws Exception {
        Batch batch = new Batch();
        long startTime = System.currentTimeMillis();
        int start = 0, end = Param.MAX_TIME;
        String fileNameDriver = "test/sample/drs" + sampleIndex + "/d/drivers_t" + start + ".txt";
        batch.updateDrivers(fileNameDriver);
        for (int i = start; i < end; i++) {
            String fileNamePassenger = "test/sample/drs" + sampleIndex + "/p/passengers_t" + i + ".txt";
            batch.updatePassenger(fileNamePassenger, sampleIndex);
        }
        int waitingDriverNum = batch.driverList.size();
        int waitingPassengerNum = batch.passengerList.size();
        batch.matching = new Match(batch.driverList, batch.passengerList);

        Solution solution = batch.matching.match(batch.curTime, Param.MATCH_ALGO, Param.MATCH_MODEL);
//            System.out.println(System.currentTimeMillis() - time);
        double time_cost = Param.getTimecost(startTime);
        System.out.printf("%d\t%d  \t%.6f   \t%.3f   \t%d%n", waitingDriverNum, waitingPassengerNum, solution.profit, time_cost, batch.passengerList.size());

    }

    public static void test() {
        double[][] costMatrix = {
                {0, 12, 11},
                {12, 0, 3},
                {11, 3, 0}
        };
        BlossomAlgorithm blossomAlgorithm = new BlossomAlgorithm(costMatrix);
        int[][] result = blossomAlgorithm.generateResultMatrix();
        System.out.println(Arrays.deepToString(result));
    }
}
