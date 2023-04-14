package match;

import algo.BlossomAlgorithm;
import algo.BranchAndBound;
import algo.KMAlgorithm;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import model.*;

import java.util.ArrayList;
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
    public double[][] dpTimeMatrix;                       // 司机到顾客起点的时间
    public double[][] pp_valid_matrix;
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
        valid_matrix = new double[nDrivers][nPassengers];
        if (flag == 2) {
            calMatch();
        }
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (passenger.cur_driver != null || passenger.pre != -1) {
                    continue;
                }
                if (driver.queue.size() == 0) {
                    double eta = Param.touringMap.calTimeDistance(driver.cur_coor, passenger.origin_coor);
                    if (passenger.next != -1) {
                        if (eta <= Param.MAX_ETA) {
                            valid_matrix[i][j] = (1 - eta / Param.MAX_ETA);
                            valid_matrix[i][j] += pp_valid_matrix[j][passenger.next];
                        }
                    }else {
                        if (eta <= Param.MAX_ETA) {
                            valid_matrix[i][j] = (1 - eta / Param.MAX_ETA);
                        }
                    }
                }else if (flag > 0 && driver.queue.size() == 1 && passenger.next == -1){
                    Passenger passenger1 = driver.queue.getFirst();
                    double eta = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger.origin_coor);
                    if ((Param.touringMap.inEllipsoid(passenger1, passenger) || Param.touringMap.allInEllipsoid(passenger1, passenger)) && eta <= Param.MAX_ETA2) {
                        double similarity = Param.touringMap.calSimilarity(passenger1, passenger);
                        if (similarity > 0) {
                            valid_matrix[i][j] += Param.samePlus;
                            valid_matrix[i][j] += Param.touringMap.calSimilarity(passenger1, passenger);
                            valid_matrix[i][j] += 1 - eta / Param.MAX_ETA2;
                        }
                    }
                }
            }
        }
    }
    public void calValidSeeker() {
        valid_matrix = new double[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            if (driver.queue.size() == 1) {
                for (int j = 0; j < nPassengers; j++) {
                    Passenger passenger = passengerList.get(j);
                    if (passenger.cur_driver != null || passenger.pre != -1 || passenger.next != -1) {
                        continue;
                    }
                    Passenger passenger1 = driver.queue.getFirst();
                    double eta = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger.origin_coor);
                    if ((Param.touringMap.inEllipsoid(passenger1, passenger) || Param.touringMap.allInEllipsoid(passenger1, passenger)) && eta <= Param.MAX_ETA2) {
                        double similarity = Param.touringMap.calSimilarity(passenger1, passenger);
                        if (similarity > 0) {
                            valid_matrix[i][j] += Param.samePlus;
                            valid_matrix[i][j] += Param.touringMap.calSimilarity(passenger1, passenger);
                            valid_matrix[i][j] += 1 - eta / Param.MAX_ETA2;
                        }
                    }
                }
            }
        }
    }
    public void calMatch() {
        pp_valid_matrix = new double[nPassengers][nPassengers];
        for (int j = 0; j < nPassengers; j++) {
            Passenger passenger1 = passengerList.get(j);
            if (passenger1.cur_driver != null) {
                continue;
            }
            for (int jj = j + 1; jj < nPassengers; jj++) {
                 Passenger passenger2 = passengerList.get(jj);
                 if (passenger2.cur_driver != null) {
                     continue;
                 }
                 double eta2 = Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
                 if (eta2 <= Param.MAX_ETA2) {
                     double same1 = 0, same2 = 0;
                     if (Param.touringMap.inEllipsoid(passenger1, passenger2) || 
                             Param.touringMap.allInEllipsoid(passenger1, passenger2)) {
                         same1 = Param.touringMap.calSimilarity(passenger1, passenger2);
                     }
                     if (Param.touringMap.inEllipsoid(passenger2, passenger1) || 
                             Param.touringMap.allInEllipsoid(passenger2, passenger1)) {
                         same2 = Param.touringMap.calSimilarity(passenger2, passenger1);
                     }
                     if (same1 > 0 && (same2 >= same1 || same2 == 0)) {
                         pp_valid_matrix[j][jj] = (1 - eta2 / Param.MAX_ETA2 + same1 + Param.samePlus);
                     }else if (same2 > 0 && (same1 > same2 || same1 == 0)) {
                         pp_valid_matrix[jj][j] = (1 - eta2 / Param.MAX_ETA2 + same2 + Param.samePlus);
                     }
                 }
            }
        }
        long start = System.currentTimeMillis();
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("SCIP");
        MPVariable[][] variables = new MPVariable[nPassengers][nPassengers];
        MPConstraint[] constraints = new MPConstraint[nPassengers];
        MPObjective obj = solver.objective();
        obj.setMaximization();
        for (int i = 0; i < nPassengers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (pp_valid_matrix[i][j] > 0) {
                    variables[i][j] = solver.makeVar(0, 1, true, i + "," + j);
                    obj.setCoefficient(variables[i][j], pp_valid_matrix[i][j]);
                }
            }
        }
        for (int i = 0; i < nPassengers; i++) {
            constraints[i] = solver.makeConstraint(0 , 1);
            for (int index = 0; index < nPassengers; index++) {
                if (pp_valid_matrix[i][index] > 0) {
                    constraints[i].setCoefficient(variables[i][index], 1);
                }
                if (pp_valid_matrix[index][i] > 0) {
                    constraints[i].setCoefficient(variables[index][i], 1);
                }
            }
        }
        solver.solve();
        for (int j = 0; j < nPassengers; j++) {
            for (int jj = 0; jj < nPassengers; jj++) {
                if (pp_valid_matrix[j][jj] > 0 && (int)variables[j][jj].solutionValue() == 1) {
                    Passenger passenger1 = passengerList.get(j);
                    Passenger passenger2 = passengerList.get(jj);
                    passenger1.next = jj;
                    passenger2.pre = j;
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
            solution = match_zkj(match_flag);
        }
        if (algo_flag == 3) {
            solution = match_ortools();
        }
        if (algo_flag == 4) {
            solution = match_cplex();
        }
//        remove(solution, cur_time);
        return solution;
    }

    public Solution match_zkj(int match_flag) throws Exception {
        Solution solution = new Solution();
        int[] size = new int[nDrivers];
        for (int i = 0; i < nDrivers; i++) {
            size[i] = driverList.get(i).queue.size();
        }
        calValidSeeker();
        KMAlgorithm km = new KMAlgorithm(valid_matrix);
        match_matrix = km.getMatch();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                }
            }
        }
        calValid(match_flag);
        km = new KMAlgorithm(valid_matrix);
        match_matrix = km.getMatch();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                    if (passenger.next != -1) {
                        Passenger passenger2 = passengerList.get(passenger.next);
                        driver.queue.add(passenger2);
                        passenger2.cur_driver = driver;
                    }
                }
            }
        }
        for (Passenger passenger : passengerList) {
            if (passenger.cur_driver == null) {
                passenger.next = -1;
                passenger.pre = -1;
            }
        }
        calValid(match_flag - 1);
        km = new KMAlgorithm(valid_matrix);
        match_matrix = km.getMatch();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (match_matrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    driver.queue.add(passenger);
                    passenger.cur_driver = driver;
                    if (passenger.next != -1) {
                        Passenger passenger2 = passengerList.get(passenger.next);
                        driver.queue.add(passenger2);
                        passenger2.cur_driver = driver;
                    }
                }
            }
        }
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            if (driver.queue.size() > size[i]) {
                Passenger passenger1 = driver.queue.getFirst();
                Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                Pattern pattern = new Pattern(driver, passenger1, passenger2);
                double eta1 = Param.touringMap.calTimeDistance(driver.cur_coor, passenger1.origin_coor);
                double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.origin_coor, passenger2.origin_coor);
                pattern.setAim(passenger2 == null ? 0 : Param.touringMap.calSimilarity(passenger1, passenger2), eta1, eta2);
                solution.patterns.add(pattern);
                solution.profit += pattern.aim;
            }
        }
        solution.checkSolution();
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
        sol.checkSolution();
        return sol;
    }
    public Solution match_cplex() throws Exception{
        IloCplex model = new IloCplex();
        double precision = Param.EPS;
        model.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, precision);
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, precision);
        model.setOut(null);
        //System.out.println(model.getVersion());
        int driver_num = driverList.size();
        int passenger_num = passengerList.size();

        //决策变量
        IloNumVar[][] match = new IloNumVar[driver_num][passenger_num];
        for (int i = 0; i < driver_num; i++) {
            for (int j = 0; j < passenger_num; j++) {
                if (valid_matrix[i][j] > 0) {
                    match[i][j] = model.boolVar();
                }
            }
        }

        //目标函数
        //将匹配权重作为最大化目标函数
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
        match_matrix = new int[nDrivers][nPassengers];
        Solution solution = new Solution();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
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
    
    public Solution match_ortools() {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        MPVariable[][] match = new MPVariable[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    match[i][j] = solver.makeVar(0, 1,false, i + "," + j);
                }
            }
        }
        for (int i = 0; i < nDrivers; i++) {
            MPConstraint d = solver.makeConstraint(0, 1, "d" + i);
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    d.setCoefficient(match[i][j], 1);
                }
            }
        }

        for (int j = 0; j < nPassengers; j++) {
            MPConstraint p = solver.makeConstraint(0, 1, "p" + "j");
            for (int i = 0; i < nDrivers; i++) {
                if (valid_matrix[i][j] > 0) {
                    p.setCoefficient(match[i][j], 1);
                }
            }
        }

        MPObjective obj = solver.objective();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    obj.setCoefficient(match[i][j], valid_matrix[i][j]);
                }
            }
        }
        obj.setMaximization();
        solver.solve();
        Solution solution = new Solution();
        match_matrix = new int[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (valid_matrix[i][j] > 0) {
                    double val = match[i][j].solutionValue();
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
