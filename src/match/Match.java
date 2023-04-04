package match;

import algo.BranchAndBound;
import common.Param;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import map.GISMap;
import map.TestMap;
import map.TouringMap;
import model.*;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class Match {
    int nDrivers;
    int nPassengers;
    long cur_time;
    int leave_count;
    public List<Driver> driverList;
    public List<Passenger> passengerList;

    double[][] valid_matrix;
    int[][] match_matrix;

    boolean[] visited;
    int[] match;
    public double[][] ppValidMatrix;                      // 顾客到顾客是否能拼车成功地计算, 0: 无法拼车成功 >0: 拼车成功后共同里程相似度
    public double[][] dpValidMatrix;                      // 接乘一个顾客的司机到第二个顾客是否能拼车成功的计算，0: 无法拼车成 >0: 拼车成功后共同里程相似度
    public double[][] ppTimeMatrix;                       //
    public double[][] dpTimeMatrix;                       // 司机到顾客起点地时间
    
    public double[][] valid_matrix1;
    public double[][][] valid_matrix2;
    Solution solution;
    public Match(List<Driver> drivers, List<Passenger> passengers) {
        driverList = drivers;
        passengerList = passengers;
        //leave_count = removeInvalid(cur_time);
        this.nDrivers = driverList.size();
        this.nPassengers = passengerList.size();
        solution = new Solution();
    }

    public void calValid(int flag) {//flag == 1 考虑拼车，其他：不考虑
        int i = 0;
        for (Driver driver : driverList) {
            int j = 0;
            for (Passenger passenger : passengerList) {
                if (driver.queue.size() == 0) {
                    double eta = Param.touringMap.calTimeDistance(driver.cur_coor, passenger.origin_coor);
                    if (eta <= Param.MAX_ETA) {
                        valid_matrix[i][j] = 1 - eta / Param.MAX_ETA;
                    }
                }else if (flag == 1 && driver.queue.size() == 1){
                    Passenger passenger1 = driver.queue.peek();
                    double eta = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger.origin_coor);
                    if ((Param.touringMap.inEllipsoid(passenger1, passenger) ||
                            Param.touringMap.allInEllipsoid(passenger1, passenger)) && eta < Param.MAX_ETA2) {
                        valid_matrix[i][j] += 2;
                        double similarity = Param.touringMap.calSimilarity(passenger1, passenger);
                        if (similarity == 0) {
                            valid_matrix[i][j] = 0;
                        }else {
                            valid_matrix[i][j] += Param.touringMap.calSimilarity(passenger1, passenger);
                            valid_matrix[i][j] += 1 - eta / Param.MAX_ETA2;
                        }
                    }
                }
                j++;
            }
            i++;
        }
    }
    public void calValid2(int flag) {
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = i + 1; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                if ((Param.touringMap.inEllipsoid(passenger1, passenger2)
                        || Param.touringMap.allInEllipsoid(passenger1, passenger2)
                        || Param.touringMap.allInEllipsoid(passenger2, passenger1)
                        && Param.touringMap.calTimeDistance(passenger1.cur_coor, passenger2.origin_coor) <= Param.MAX_ETA2)) {
                    for (int k = 0; k < nDrivers; k++) {
                        double eta1 = Param.touringMap.calTimeDistance(driverList.get(i).cur_coor, passenger1.origin_coor);
                        double eta2 = Param.touringMap.calTimeDistance(driverList.get(i).cur_coor, passenger2.origin_coor);
                        
                    }
                }
            }
        }
    }
    void calPPValid() {
        long s = System.currentTimeMillis();
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                ppTimeMatrix[i][j] = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
                if (Param.touringMap.inEllipsoid(passenger1, passenger2) || Param.touringMap.allInEllipsoid(passenger1, passenger2)) {
                    ppValidMatrix[i][j] = Param.touringMap.calSimilarity(passenger1, passenger2);
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
                dpTimeMatrix[i][j] = Param.touringMap.calTimeDistance(driver.cur_coor, passenger.origin_coor); // Todo: ?
                // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                if (driverList.get(i).queue.size() > 0) {
                    Passenger passenger0 = driverList.get(i).queue.getFirst();
                    dpTimeMatrix[i][j] = Param.touringMap.calTimeDistance(passenger0.origin_coor, passenger.origin_coor);
                    if (Param.touringMap.inEllipsoid(passenger0, passenger) || Param.touringMap.allInEllipsoid(passenger0, passenger)) {
                        dpValidMatrix[i][j] = Param.touringMap.calSimilarity(passenger0, passenger);
                    }
                }
            }
        }
    }
    
    public Solution match(long cur_time, int algo_flag, int match_flag) throws Exception {
        Solution solution = null;
        if (algo_flag == 1) {
            this.ppValidMatrix = new double[nPassengers][nPassengers];
            this.dpValidMatrix = new double[nDrivers][nPassengers];
            this.ppTimeMatrix = new double[nPassengers][nPassengers];
            this.dpTimeMatrix = new double[nDrivers][nPassengers];
            calPPValid();
            calDPValid();
            solution = match_zjr(cur_time, match_flag);
        }
        if (algo_flag == 2) {
            match_matrix = new int[nDrivers][nPassengers];
            valid_matrix = new double[nDrivers][nPassengers];
            calValid(match_flag);
            solution = match_zkj();
        }
        if (algo_flag == 3) {
            match_matrix = new int[nDrivers][nPassengers];
            valid_matrix = new double[nDrivers][nPassengers];
            calValid(match_flag);
            solution = match_ortools();
        }
        remove(solution, cur_time);
        return solution;
    }
    public Solution match_ortools() {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("SCIP");
        MPVariable[][] d1 = new MPVariable[nDrivers][nPassengers];
        MPVariable[][] d2 = new MPVariable[nDrivers][nPassengers];
        MPVariable[][] dp = new MPVariable[nDrivers][nPassengers];
        MPVariable[][] pp = new MPVariable[nPassengers][nPassengers];
        boolean isIP = true;
        for (int j = 0; j < nPassengers; j++) {
            for (int i = 0; i < nDrivers; i++) {
                if (driverList.get(i).queue.size() > 0) {
                    if (dpValidMatrix[i][j] > 0 && dpTimeMatrix[i][j] <= Param.MAX_ETA2) {
                        dp[i][j] = solver.makeVar(0, 1, isIP, i + " dp " + j);
                    }
                }else {
                    if (dpTimeMatrix[i][j] <= Param.MAX_ETA) {
                        MPConstraint constraint = solver.makeConstraint(0, Double.POSITIVE_INFINITY);
                        d1[i][j] = solver.makeVar(0, 1, isIP, i + " d1 " + j);
                        d2[i][j] = solver.makeVar(0, 1, isIP, i + " d2 " + j);
                        constraint.setCoefficient(d1[i][j], 1);
                        constraint.setCoefficient(d2[i][j], -1);
                    }
                }
            }
        }
        for (int j = 0; j < nPassengers; j++) {
            for (int jj = 0; jj < nPassengers; jj++) {
                if (ppValidMatrix[j][jj] > 0) {
                    pp[j][jj] = solver.makeVar(0, 1, isIP, j + " pp " + jj);
                    for (int i = 0; i < nDrivers; i++) {
                        MPConstraint constraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1);
                        constraint.setCoefficient(d1[i][j], 1);
                        constraint.setCoefficient(d2[i][jj], 1);
                        constraint.setCoefficient(pp[j][jj], -1);
                    }
                }
            }
        }
        
        for (int i = 0; i < nDrivers; i++) {
            MPConstraint d = solver.makeConstraint(0, 1, "d" + i);
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    d.setCoefficient(pp[i][j], 1);
                }
            }
        }
        
        for (int j = 0; j < nPassengers; j++) {
            MPConstraint p = solver.makeConstraint(0, 1, "p" + "j");
            for (int i = 0; i < nDrivers; i++) {
                if (valid_matrix[i][j] > 0) {
                    p.setCoefficient(pp[i][j], 1);
                }
            }
        }
        
        MPObjective obj = solver.objective();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    obj.setCoefficient(pp[i][j], valid_matrix[i][j]);
                }
            }
        }
        obj.setMaximization();
        solver.solve();
        Solution solution = new Solution();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    double val = pp[i][j].solutionValue();
                    if (Param.equals(val, 1)) {
                        match_matrix[i][j] = 1;
                    }
                }
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                    Passenger passenger1 = driver.queue.getFirst();
                    Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                    Pattern pattern = new Pattern(driver, passenger1, passenger2);
                    double eta1 = Param.touringMap.calTimeDistance(passenger1.origin_coor, driver.cur_coor);
                    double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
                    pattern.setAim(passenger2 == null ? 0 : Param.touringMap.calSimilarity(passenger1, passenger2), eta1, eta2);
                    solution.patterns.add(pattern);
                    solution.profit += pattern.aim;
                }
            }
        }
        return solution;
    }
    

    public Solution match_zkj() throws Exception {
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
        IloNumVar[][] match = new IloNumVar[driver_num][passenger_num];
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                if (valid_matrix[i][j] > 0) {
                    match[i][j] = model.boolVar();
                }
            }
        }

        //目标函数
        //将匹配成功数作为最大化目标函数
        IloNumExpr obj = model.linearNumExpr();
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                if (valid_matrix[i][j] > 0) {
                    obj = model.sum(obj, model.prod(match[i][j], valid_matrix[i][j]));
                }
            }
        }
        //将带权路径总和作为最大化目标函数

        model.addMaximize(obj);
        
        //一个司机最多匹配一个乘客
        for (int i = 0; i < driver_num; i++) {
            IloNumExpr expr = model.linearNumExpr();
            for (int j = 0; j < passenger_num; j++) {
                if (valid_matrix[i][j] > 0) {
                    expr = model.sum(expr, match[i][j]);
                }
            }
            model.addLe(expr, 1);
        }
        //一个乘客最多匹配一个司机
        for (int j = 0; j < passenger_num; j++) {
            IloNumExpr expr = model.linearNumExpr();
            for (int i = 0; i < driver_num; i++) {
                if (valid_matrix[i][j] > 0) {
                    expr = model.sum(expr, match[i][j]);
                }
            }
            model.addLe(expr, 1);
        }
        
        model.solve();
        
        Solution solution = new Solution();
        for (int i = driver_num - 1; i >= 0; i--) {
            for (int j = passenger_num - 1; j >= 0; j--) {
                if (valid_matrix[i][j] > 0) {
                    match_matrix[i][j] = (int) model.getValue(match[i][j]);
                }
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
         //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                    Passenger passenger1 = driver.queue.getFirst();
                    Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                    Pattern pattern = new Pattern(driver, passenger1, passenger2);
                    double eta1 = Param.touringMap.calTimeDistance(passenger1.origin_coor, driver.cur_coor);
                    double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
                    pattern.setAim(passenger2 == null ? 0 : Param.touringMap.calSimilarity(passenger1, passenger2), eta1, eta2);
                    solution.patterns.add(pattern);
                    solution.profit += pattern.aim;
                }
            }
        }
        return solution;
    }

    public Solution match_zjr(long cur_time, int match_flag) throws IloException {
        Instance inst = new Instance(cur_time, match_flag, driverList, passengerList, ppValidMatrix, dpValidMatrix, ppTimeMatrix, dpTimeMatrix);
        BranchAndBound bnp = new BranchAndBound(inst);
        bnp.run();
        Solution sol = bnp.bestSol;
        for (Pattern pattern : sol.patterns) {
            Driver driver = pattern.driver;
            Passenger passenger1 = pattern.passenger1;
            Passenger passenger2 = pattern.passenger2;
            if (pattern.passenger1Id >= 0 && pattern.passenger1Id == passenger1.ID) {
                driver.queue.add(passenger1);
            }
            if (pattern.passenger2Id >= 0 && pattern.passenger2Id == passenger2.ID) {
                driver.queue.add(passenger2);
            }
            if (pattern.driver.queue.size() > 0 && pattern.driver.match_coor == null) {
                pattern.driver.saveMatch_coor();
            }
        }
        return sol;
    }
    public int removeInvalid(long cur_time) {
        List<Passenger> removePassengers = new ArrayList<>();
        for (Passenger passenger : passengerList) {
            if (cur_time - passenger.submit_time >= passenger.expected_arrive_time * Param.LEAVING_COFF) {
                removePassengers.add(passenger);
            }
        }
        passengerList.removeAll(removePassengers);
        return removePassengers.size();
    }

    public void remove(Solution sol, long cur_time) {
        if (sol == null) {
            return;
        }
        List<Driver> removeDrivers = new ArrayList<>();
        List<Passenger> removePassengers = new ArrayList<>();
        for (Driver driver : driverList) {
            for (Pattern pattern : sol.patterns) {
                if (pattern.driverId == driver.ID && pattern.passenger2Id >= 0) {
                    removeDrivers.add(driver);
                }
            }
        }
        for (Passenger passenger : passengerList) {
            passenger.renew(cur_time);
            for (Pattern pattern : sol.patterns) {
                if (pattern.passenger1Id == passenger.ID || pattern.passenger2Id == passenger.ID) {
                    removePassengers.add(passenger);
                }
            }
        }
        driverList.removeAll(removeDrivers);
        passengerList.removeAll(removePassengers);
    }
    public double calProfit(Solution sol) {
        double profit = 0.0;
        for (Pattern pattern : sol.patterns) {
            Driver driver = pattern.driver;
            Passenger passenger1 = pattern.passenger1;
            // 接两个乘客
            if (pattern.passenger2Id >= 0) {

            } else {
            // 接一个乘客
                double etaAim = Param.touringMap.calTimeDistance(driver.cur_coor, passenger1.origin_coor);
                double sameAim = 0.0;
                double aim = (sameAim > 0 ? (sameAim + 2) : 0) + 2 - etaAim/Param.MAX_ETA;
                profit += aim;
            }
        }
        return profit;
    }
}
