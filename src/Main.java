import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.NoBatchifyTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.TranslatorContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.ortools.Loader;
import common.Param;
import match.Batch;
import match.Match;
import model.Pattern;
import model.Solution;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        Param.COUNT = 0;
        Param.MAX_TIME = 50;
        Param.setMapChoose(2);
        int timeInterval = 10;
        runSample(timeInterval, 2);
        System.out.println(Param.COUNT);
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
        Loader.loadNativeLibraries();
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
                batch.updatePassenger(fileNamePassenger, i);
            }
            int size2 = batch.passengerList.size();
            int waitingDriverNum = batch.driverList.size();
            int waitingPassengerNum = batch.passengerList.size();
            batch.matching = new Match(batch.driverList, batch.passengerList);
            batch.curTime += timeInterval;
            Solution curSolution = batch.matching.match(batch.curTime, Param.MATCH_ALGO, Param.MATCH_MODEL);
//            System.out.println(System.currentTimeMillis() - time);
            solution.profit += curSolution.profit;
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
            System.out.printf("第%d个阶段，待匹配司机数为%d，待匹配乘客数为%d，匹配成功对数为%d，" +
                            "当前阶段剩余司机数为%d，剩余乘客数为%d，取消订单乘客数为%d，求解总消耗时长%d毫秒",
                    end / timeInterval, waitingDriverNum, waitingPassengerNum, result,
                    batch.driverList.size(), batch.passengerList.size(), curSolution.leaveCount, end_time - start_time);
            System.out.println();

        }
        //solution.outputSolution(sample_index);
        System.out.printf("总乘客数目为%d，匹配成功的乘客数为%d，未匹配成功的乘客数为%d, 未上车的乘客数为%d，取消订单乘客数为%d，拼车成功率为%.2f%%",
                passengerSum, matchSum, passengerSum - matchSum - batch.passengerList.size() - solution.leaveCount,
                batch.passengerList.size(), solution.leaveCount, (double) matchSum / passengerSum * 100);
        System.out.println();
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
        double time_cost = Param.getTimeCost(startTime);
        System.out.printf("%d\t%d  \t%.6f   \t%.3f   \t%d%n", waitingDriverNum, waitingPassengerNum, solution.profit, time_cost, batch.passengerList.size());

    }

    public static double testNetMap(double lat1, double lng1, double lat2, double lng2) throws MalformedModelException, IOException, TranslateException {
        Logger logger = (Logger) LoggerFactory.getLogger("ai.djl");
        logger.setLevel(Level.ERROR);
        Path modelDir = Paths.get("MapLearning\\model\\NetMap2.pt");
        Model model = Model.newInstance("test");
        model.load(modelDir);
        Predictor<double[], Double> predictor = model.newPredictor(new NoBatchifyTranslator<double[], Double>() {
            @Override
            public Double processOutput(TranslatorContext translatorContext, NDList ndList) throws Exception {
                return ndList.get(0).getDouble();
            }

            @Override
            public NDList processInput(TranslatorContext translatorContext, double[] floats) throws Exception {
                NDManager ndManager = translatorContext.getNDManager();
                NDArray ndArray = ndManager.create(floats);
                return new NDList(ndArray);
            }
        });
        return predictor.predict(new double[]{lat1, lng1, lat2, lng2});
    }
}