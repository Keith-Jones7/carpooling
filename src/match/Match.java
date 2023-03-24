package match;

import java.lang.reflect.Array;
import java.util.*;
import algo.BranchAndBound;
import common.Param;
import ilog.concert.IloException;
import ilog.concert.IloIntExpr;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;
import map.TestMap;
import model.*;
import map.GISMap;

import java.util.List;
public class Match {
    int nDrivers;
    int nPassengers;
    public List<Driver> driverList;
    public List<Passenger> passengerList;

    double[][] valid_matrix;
    int[][] match_matrix;

    public double[][] ppValidMatrix;                      // 顾客到顾客是否能拼车成功地计算, 0: 无法拼车成功 >0: 拼车成功后共同里程相似度
    public double[][] dpValidMatrix;                      // 接乘一个顾客的司机到第二个顾客是否能拼车成功的计算，0: 无法拼车成 >0: 拼车成功后共同里程相似度
    public double[][] ppTimeMatrix;                       //
    public double[][] dpTimeMatrix;                        // 司机到顾客起点地时间
    private static final TestMap map = new TestMap();
    //private static final map.GISMap map = new GISMap();
    Solution solution;
    public Match(List<Driver> drivers, List<Passenger> passengers) {
        driverList = drivers;
        passengerList = passengers;
        this.nDrivers = driverList.size();
        this.nPassengers = passengerList.size();
        this.ppValidMatrix = new double[nPassengers][nPassengers];
        this.dpValidMatrix = new double[nDrivers][nPassengers];
        this.ppTimeMatrix = new double[nPassengers][nPassengers];
        this.dpTimeMatrix = new double[nDrivers][nPassengers];
        match_matrix = new int[drivers.size()][passengers.size()];
        valid_matrix = new double[drivers.size()][passengers.size()];
        calValid();
        calPPValid();
        calDPValid();
        solution = new Solution();
    }

    public void calValid() {
        int i = 0;
        for (Driver driver : driverList) {
            int j = 0;
            for (Passenger passenger : passengerList) {
                long start_time = System.currentTimeMillis();
                if (driver.queue.size() == 0) {
                    double eta = map.calTimeDistance(driver.cur_coor, passenger.origin_coor);
                    //System.out.println(map.calTimeDistance(driver.cur_coor, passenger.origin_coor));
                    if (eta <= Param.MAX_ETA) {
                        valid_matrix[i][j] = 2 - (eta) / Param.MAX_ETA;
                    }else {
                        valid_matrix[i][j] = 0;
                    }
                }else if (driver.queue.size() == 1){
                    Passenger passenger1 = driver.queue.peek();
                    if (map.inEllipsoid(passenger1, passenger) ||
                            map.allInEllipsoid(passenger1, passenger)) {
                        valid_matrix[i][j] += 2;
                        double similarity = map.calSimilarity(passenger1, passenger);
                        if (similarity == 0) {
                            valid_matrix[i][j] = 0;
                        }else {
                            valid_matrix[i][j] += map.calSimilarity(passenger1, passenger);
                        }
                    }else {
                        valid_matrix[i][j] = 0;
                    }
                }
                long end_time = System.currentTimeMillis();
                j++;
            }
            i++;
        }
    }

    void calPPValid() {
        long s = System.currentTimeMillis();
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                ppTimeMatrix[i][j] = map.calTimeDistance(passenger1.cur_coor, passenger2.origin_coor);
                if (map.inEllipsoid(passenger1, passenger2) || map.allInEllipsoid(passenger1, passenger2)) {
                    ppValidMatrix[i][j] = map.calSimilarity(passenger1, passenger2);
                }
            }
        }
    }

    void calDPValid() {
        long s = System.currentTimeMillis();
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                dpTimeMatrix[i][j] = map.calTimeDistance(driver.cur_coor, passenger.origin_coor); // Todo: ?
                // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                if (driverList.get(i).queue.size() > 0) {
                    Passenger passenger0 = driverList.get(i).queue.peek();
                    if (map.inEllipsoid(passenger0, passenger) || map.allInEllipsoid(passenger0, passenger)) {
                        dpValidMatrix[i][j] = map.calSimilarity(passenger0, passenger);
                    }
                }
            }
        }
    }

    public int match(long cur_time, int flag) throws Exception {
        if (flag == 1) {
            return match_zjr(cur_time);
        }
        if (flag == 2) {
            return match_zkj(cur_time);
        }
        return -1;
    }
    public int match_zkj(long cur_time) throws Exception {
        IloCplex model = new IloCplex();
        double precision = 1e-5;
        model.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, precision);
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, precision);
        model.setOut(null);
        //System.out.println(model.getVersion());
        int driver_num = driverList.size();
        int passenger_num = passengerList.size();

        //决策变量
        //匹配成功数最大
        IloIntVar[][] match = new IloIntVar[driver_num][passenger_num];
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                match[i][j] = model.intVar(0, 1, "M" + i + "," + j);
            }
        }

        //目标函数
        //将匹配成功数作为最大化目标函数
        IloNumExpr obj = model.intExpr();
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                obj = model.sum(obj, model.prod(match[i][j], valid_matrix[i][j]));
            }
        }
        //将带权路径总和作为最大化目标函数

        model.addMaximize(obj);
        //约束条件

        //一个司机最多匹配一个乘客
        for (int i = 0; i < driver_num; i++) {
            IloIntExpr expr = model.intExpr();
            for (int j = 0; j < passenger_num; j++) {
                expr = model.sum(expr, match[i][j]);
            }
            model.addLe(expr, 1);
        }
        //一个乘客最多匹配一个司机
        for (int j = 0; j < passenger_num; j++) {
            IloIntExpr expr = model.intExpr();
            for (int i = 0; i < driver_num; i++) {
                expr = model.sum(expr, match[i][j]);
            }
            model.addLe(expr, 1);
        }
        //可行约束
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                IloIntExpr expr = model.intExpr();
                expr = model.sum(expr, match[i][j]);
                model.addLe(expr, valid_matrix[i][j]);
            }
        }
        model.solve();
        int count = 0;
        for (int i = driver_num - 1; i >= 0; i--) {
            for (int j = passenger_num - 1; j >= 0; j--) {
                match_matrix[i][j] = (int) model.getValue(match[i][j]);
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
         //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                }
            }
        }
        Solution solution = new Solution();

        for (int i = driver_num - 1; i >= 0; i--) {
            Driver driver = driverList.get(i);
            if (driverList.get(i).queue.size() == 2) {
                count++;
                solution.patterns.add(new Pattern(driver, driver.queue.getFirst(), driver.queue.getLast()));
                driverList.remove(i);
            }else if (driver.queue.size() == 1) {
                //solution.patterns.add(new Pattern(driver, driver.queue.getFirst(), null));
            }
        }
        for (int j = passenger_num - 1; j >= 0; j--) {
            Passenger passenger = passengerList.get(j);
            passenger.renew(cur_time);
            if (passenger.cur_driver != null) {
                passengerList.remove(j);
            }
        }
        solution.outputSolution(cur_time);
        return count;
    }

    public int match_zjr(long cur_time) throws IloException {
        Instance inst = new Instance(driverList, passengerList, ppValidMatrix, dpValidMatrix, ppTimeMatrix, dpTimeMatrix );
        BranchAndBound bnp = new BranchAndBound(inst);
        bnp.run();
        Solution sol = bnp.bestSol;
        int cnt = 0;
        for (Pattern pattern : sol.patterns) {
            if (pattern.passenger1Id >= 0) {
                driverList.get(pattern.driverId).queue.add(passengerList.get(pattern.passenger1Id));
            }
            if (pattern.passenger2Id >= 0) {
                driverList.get(pattern.driverId).queue.add(passengerList.get(pattern.passenger2Id));
                cnt++;
            }
        }
        for (int j = 0; j < nPassengers; j++) {
            Passenger passenger = passengerList.get(j);
            passenger.renew(cur_time);
        }
        // remove drivers and passengers
        List<Driver> removeDrivers = new ArrayList<>();
        List<Passenger> removePassengers = new ArrayList<>();
        for (int i = 0; i < nDrivers; i++) {
            for (Pattern pattern : sol.patterns) {
                if (pattern.driverId == i && pattern.passenger2Id >= 0) {
                    removeDrivers.add(driverList.get(i));
                }
            }
        }
        for (int j = 0; j < nPassengers; j++) {
            for (Pattern pattern : sol.patterns) {
                if (pattern.passenger1Id == j) {
                    removePassengers.add(passengerList.get(j));
                }
                if (pattern.passenger2Id == j) {
                    removePassengers.add(passengerList.get(j));
                }
            }
        }
        driverList.removeAll(removeDrivers);
        passengerList.removeAll(removePassengers);

        return cnt;
    }

    // Todo: 将剩余为拼车顾客安排司机
    public void railMatch() {

    }
}
