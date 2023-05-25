package match;

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
import ilog.cplex.IloCplex;
import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Match {
    public List<Driver> driverList;
    public List<Passenger> passengerList;
    public double[][] ppValidMatrix;                      // 顾客到顾客是否能拼车成功地计算, 0: 无法拼车成功 >0: 拼车成功后共同里程相似度
    public double[][] dpValidMatrix;                      // 接乘一个顾客的司机到第二个顾客是否能拼车成功的计算，0: 无法拼车成 >0: 拼车成功后共同里程相似度
    public double[][] ppTimeMatrix;                       //
    public double[][] dpTimeMatrix;                       // 司机到顾客起点的时间
    public double[][] ppValidMatrix2;
    int nDrivers;
    int nPassengers;
    int leave_count;
    double[][] validMatrix;
    int[][] matchMatrix;
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
        long s = System.currentTimeMillis();
        validMatrix = new double[nDrivers][nPassengers];    
        ExecutorService executor = Executors.newFixedThreadPool(nDrivers * nPassengers);
        List<Future<Double>> futures = new ArrayList<>();
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (passenger.curDriver != null || passenger.pre != -1) {
                    continue;
                }
                if (driver.queue.size() == 0 && Param.testMap.calTimeDistance(driver.curCoor, passenger.originCoor) <= Param.MAX_ETA) {
                    Param.COUNT++;
                    Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(driver.curCoor, passenger.originCoor);
                    futures.add(executor.submit(etaCalculator));
                }else if (flag > 0 && driver.queue.size() == 1 && passenger.next == -1) {
                    Passenger passenger1 =  driver.queue.getFirst();
                    if (Param.testMap.calTimeDistance(passenger1.originCoor, passenger.originCoor) <= Param.MAX_ETA2) {
                        Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(passenger1.originCoor, passenger.originCoor);
                        futures.add(executor.submit(etaCalculator));
                    }
                }
            }
        }
        executor.shutdown();
        int index = 0;
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (passenger.curDriver != null || passenger.pre != -1) {
                    continue;
                }
                if (driver.queue.size() == 0 && Param.testMap.calTimeDistance(driver.curCoor, passenger.originCoor) <= Param.MAX_ETA) {
                    double eta = Param.MAX_ETA;
                    try {
                        eta = futures.get(index++).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    if (passenger.next != -1) {
                        if (eta <= Param.MAX_ETA) {
                            validMatrix[i][j] = (1 - eta / Param.MAX_ETA);
                            validMatrix[i][j] += ppValidMatrix2[j][passenger.next];
                        }
                    } else {
                        if (eta <= Param.MAX_ETA) {
                            validMatrix[i][j] = (1 - eta / Param.MAX_ETA);
                        }
                    }
                } else if (flag > 0 && driver.queue.size() == 1 && passenger.next == -1) {
                    Passenger passenger1 = driver.queue.getFirst();
                    if (Param.testMap.calTimeDistance(passenger1.originCoor, passenger.originCoor) <= Param.MAX_ETA2) {
                        double eta = Param.MAX_ETA2;
                        try {
                            eta = futures.get(index++).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (eta <= Param.MAX_ETA2 && (Param.touringMap.inEllipsoid(passenger1, passenger) || Param.touringMap.allInEllipsoid(passenger1, passenger))) {
                            double similarity = Param.touringMap.calSimilarity(passenger1, passenger);
                            if (similarity > 0) {
                                validMatrix[i][j] += Param.samePlus;
                                validMatrix[i][j] += Param.touringMap.calSimilarity(passenger1, passenger);
                                validMatrix[i][j] += 1 - eta / Param.MAX_ETA2;
                            }
                        }
                    }
                }
            }
        }
        System.out.println(Param.getTimeCost(s));
    }

    public void calValidSeeker() {
        validMatrix = new double[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            if (driver.queue.size() == 1) {
                for (int j = 0; j < nPassengers; j++) {
                    Passenger passenger = passengerList.get(j);
                    if (passenger.curDriver != null || (passenger.pre == -1 && passenger.next == -1)) {
                        continue;
                    }
                    Passenger passenger1 = driver.queue.getFirst();
                    double eta = Param.touringMap.calTimeDistance(passenger1.originCoor, passenger.originCoor);
                    if ((Param.touringMap.inEllipsoid(passenger1, passenger) || Param.touringMap.allInEllipsoid(passenger1, passenger)) && eta <= Param.MAX_ETA2) {
                        double similarity = Param.touringMap.calSimilarity(passenger1, passenger);
                        if (similarity > 0) {
                            validMatrix[i][j] += Param.samePlus;
                            validMatrix[i][j] += Param.touringMap.calSimilarity(passenger1, passenger);
                            validMatrix[i][j] += 1 - eta / Param.MAX_ETA2;
                        }
                    }
                }
            }
        }
    }

    public void calMatch() {
        ppValidMatrix2 = new double[nPassengers][nPassengers];
        ExecutorService executor = Executors.newFixedThreadPool(nPassengers * nPassengers);
        List<Future<Double>> futures = new ArrayList<>();
        for (int j = 0; j < nPassengers; j++) {
            Passenger passenger1 = passengerList.get(j);
            if (passenger1.curDriver != null) {
                continue;
            }
            for (int jj = j + 1; jj < nPassengers; jj++) {
                Passenger passenger2 = passengerList.get(jj);
                if (passenger2.curDriver != null || Param.testMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor) > Param.MAX_ETA2) {
                    continue;
                }
                Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                Param.COUNT++;
                futures.add(executor.submit(etaCalculator));
            }
        }
        executor.shutdown();
        int index = 0;
        for (int j = 0; j < nPassengers; j++) {
            Passenger passenger1 = passengerList.get(j);
            if (passenger1.curDriver != null) {
                continue;
            }
            for (int jj = j + 1; jj < nPassengers; jj++) {
                Passenger passenger2 = passengerList.get(jj);
                if (passenger2.curDriver != null || Param.testMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor) > Param.MAX_ETA2) {
                    continue;
                }
                double eta2 = Param.MAX_ETA2;
                try {
                    eta2 = futures.get(index++).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
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
                        ppValidMatrix2[j][jj] = (1 - eta2 / Param.MAX_ETA2 + same1 + Param.samePlus);
                    } else if (same2 > 0 && (same1 > same2 || same1 == 0)) {
                        ppValidMatrix2[jj][j] = (1 - eta2 / Param.MAX_ETA2 + same2 + Param.samePlus);
                    }
                }
            }
        }
        MPSolver solver = MPSolver.createSolver("SCIP");
        MPVariable[][] variables = new MPVariable[nPassengers][nPassengers];
        MPConstraint[] constraints = new MPConstraint[nPassengers];
        MPObjective obj = solver.objective();
        obj.setMaximization();
        for (int i = 0; i < nPassengers; i++) {
            constraints[i] = solver.makeConstraint(0, 1);
            for (int j = 0; j < nPassengers; j++) {
                if (ppValidMatrix2[i][j] > 0) {
                    variables[i][j] = solver.makeVar(0, 1, true, i + "," + j);
                    obj.setCoefficient(variables[i][j], ppValidMatrix2[i][j]);
                    constraints[i].setCoefficient(variables[i][j], 1);
                }
                if (ppValidMatrix2[j][i] > 0) {
                    constraints[i].setCoefficient(variables[j][i], 1);
                }
            }
        }
        solver.solve();
        for (int j = 0; j < nPassengers; j++) {
            for (int jj = 0; jj < nPassengers; jj++) {
                if (ppValidMatrix2[j][jj] > 0 && (int) variables[j][jj].solutionValue() == 1) {
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
        ExecutorService executor = Executors.newFixedThreadPool(nPassengers * nPassengers);
        List<Future<Double>> futures = new ArrayList<>();
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                if (Param.testMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor) <= Param.MAX_ETA2) {
                    Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                    futures.add(executor.submit(etaCalculator));
                }
            }
        }
        executor.shutdown();
        ExecutorService executor1 = Executors.newFixedThreadPool(2 * nPassengers * nPassengers);
        List<Future<Boolean>> futures1 = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                if (Param.testMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor) <= Param.MAX_ETA2) {
                    double eta = Param.MAX_ETA;
                    try {
                        eta = futures.get(index++).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    ppTimeMatrix[i][j] = eta;

                    if (ppTimeMatrix[i][j] <= Param.MAX_ETA2) {
                        // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                        Callable<Boolean> etaCalculator1 = () -> Param.touringMap.inEllipsoid(passenger1, passenger2);
                        Callable<Boolean> etaCalculator2 = () -> Param.touringMap.allInEllipsoid(passenger1, passenger2);
                        futures1.add(executor1.submit(etaCalculator1));
                        futures1.add(executor1.submit(etaCalculator2));
                    }
                } else {
                    ppTimeMatrix[i][j] = 2 * Param.MAX_ETA2;
                }
            }
        }

        executor1.shutdown();
        int index1 = 0;
        for (int i = 0; i < nPassengers; i++) {
            Passenger passenger1 = passengerList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger2 = passengerList.get(j);
                if (Param.testMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor) <= Param.MAX_ETA2) {
                    if (ppTimeMatrix[i][j] <= Param.MAX_ETA2) {
                        // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                        boolean flag1 = false;
                        boolean flag2 = false;
                        try {
                            flag1 = futures1.get(index1++).get();
                            flag2 = futures1.get(index1++).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (flag1 || flag2) {
                            ppValidMatrix[i][j] = Param.touringMap.calSimilarity(passenger1, passenger2);
                        }
                    }
                }
            }
        }
        System.out.println(Param.getTimeCost(s));
    }

    void calDPValid() {
        long s = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(nDrivers * nPassengers);
        List<Future<Double>> futures = new ArrayList<>();
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (driverList.get(i).queue.size() == 0) {
                    if (Param.testMap.calTimeDistance(driver.curCoor, passenger.originCoor) <= Param.MAX_ETA) {
                        Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(driver.curCoor, passenger.originCoor);
                        futures.add(executor.submit(etaCalculator));
                    }
                } else {
                    Passenger passenger0 = driverList.get(i).queue.getFirst();
                    if (Param.testMap.calTimeDistance(passenger0.originCoor, passenger.originCoor) <= Param.MAX_ETA2) {
                        Callable<Double> etaCalculator = () -> Param.touringMap.calTimeDistance(passenger0.originCoor, passenger.originCoor);
                        futures.add(executor.submit(etaCalculator));
                    }
                }
            }
        }
        executor.shutdown();
        ExecutorService executor1 = Executors.newFixedThreadPool(2 * nDrivers * nPassengers);
        List<Future<Boolean>> futures1 = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (driverList.get(i).queue.size() == 0) {
                    if (Param.testMap.calTimeDistance(driver.curCoor, passenger.originCoor) <= Param.MAX_ETA) {
                        double eta = Param.MAX_ETA;
                        try {
                            eta = futures.get(index++).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        dpTimeMatrix[i][j] = eta;
                    } else {
                        dpTimeMatrix[i][j] = 2 * Param.MAX_ETA;
                    }
                } else {
                    Passenger passenger0 = driverList.get(i).queue.getFirst();
                    if (Param.testMap.calTimeDistance(passenger0.originCoor, passenger.originCoor) <= Param.MAX_ETA2) {
                        double eta = Param.MAX_ETA;
                        try {
                            eta = futures.get(index++).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        dpTimeMatrix[i][j] = eta;
                        // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                        Callable<Boolean> etaCalculator1 = () -> Param.touringMap.inEllipsoid(passenger0, passenger);
                        Callable<Boolean> etaCalculator2 = () -> Param.touringMap.allInEllipsoid(passenger0, passenger);
                        futures1.add(executor1.submit(etaCalculator1));
                        futures1.add(executor1.submit(etaCalculator2));
                    } else {
                        dpTimeMatrix[i][j] = 2 * Param.MAX_ETA2;
                    }
                }
            }
        }
        executor1.shutdown();
        int index1 = 0;
        for (int i = 0; i < nDrivers; i++) {
            Driver driver = driverList.get(i);
            for (int j = 0; j < nPassengers; j++) {
                Passenger passenger = passengerList.get(j);
                if (driverList.get(i).queue.size() != 0) {
                    Passenger passenger0 = driverList.get(i).queue.getFirst();
                    if (Param.testMap.calTimeDistance(passenger0.originCoor, passenger.originCoor) <= Param.MAX_ETA2) {
                        // 只有当司机带了一个顾客时，才需要计算司机到顾客的里程相似度
                        boolean flag1 = false;
                        boolean flag2 = false;
                        try {
                            flag1 = futures1.get(index1++).get();
                            flag2 = futures1.get(index1++).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (flag1 || flag2) {
                            dpValidMatrix[i][j] = Param.touringMap.calSimilarity(passenger0, passenger);
                        }
                    }
                }
            }
        }
        System.out.println(Param.getTimeCost(s));
    }

    public Solution match(long cur_time, int algo_flag, int match_flag) throws Exception {
        if (algo_flag <= 1) {
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
        //solution.checkSolution();
        remove(solution, cur_time);
//        solution.checkSolutionGIS();
        return solution;
    }

    public Solution match_zkj(int match_flag) throws Exception {
        Solution solution = new Solution();
        if (nPassengers == 0) {
            return solution;
        }
        int[] size = new int[nDrivers];
        for (int i = 0; i < nDrivers; i++) {
            size[i] = driverList.get(i).queue.size();
        }
        KMAlgorithm km;
        if (match_flag == 2) {
            calMatch();
            calMatch();
            calValid(match_flag);
            km = new KMAlgorithm(validMatrix);
            matchMatrix = km.getMatch();
            for (int i = 0; i < nDrivers; i++) {
                for (int j = 0; j < nPassengers; j++) {
                    if (matchMatrix[i][j] == 1) {
                        Driver driver = driverList.get(i);
                        Passenger passenger = passengerList.get(j);
                        driver.queue.add(passenger);
                        passenger.curDriver = driver;
                        if (passenger.next != -1) {
                            Passenger passenger2 = passengerList.get(passenger.next);
                            driver.queue.add(passenger2);
                            passenger2.curDriver = driver;
                        }
                    }
                }
            }
            for (Passenger passenger : passengerList) {
                if (passenger.curDriver == null) {
                    passenger.next = -1;
                    passenger.pre = -1;
                }
            }
        }
        calValid(match_flag);
        km = new KMAlgorithm(validMatrix);
        matchMatrix = km.getMatch();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (matchMatrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    driver.queue.add(passenger);
                    passenger.curDriver = driver;
                    if (passenger.next != -1) {
                        Passenger passenger2 = passengerList.get(passenger.next);
                        driver.queue.add(passenger2);
                        passenger2.curDriver = driver;
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
                double eta1 = size[i] == 1 ? Param.MAX_ETA : Param.touringMap.calTimeDistance(driver.curCoor, passenger1.originCoor);
                double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                pattern.setAim(passenger2 == null ? 0 : Param.touringMap.calSimilarity(passenger1, passenger2), eta1, eta2);
                solution.patterns.add(pattern);
                solution.profit += pattern.aim;
                if (driver.matchCoor == null) {
                    driver.saveMatchCoor();
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
            if (pattern.driver.queue.size() > 0 && pattern.driver.matchCoor == null) {
                pattern.driver.saveMatchCoor();
            }
        }
        return sol;
    }

    public Solution match_cplex() throws Exception {
        IloCplex model = new IloCplex();
        double precision = Param.EPS;
        model.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, precision);
        model.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, precision);
        model.setOut(null);
        //System.out.println(model.getVersion());

        //决策变量
        IloNumVar[][] match = new IloNumVar[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    match[i][j] = model.boolVar();
                }
            }
        }

        //目标函数
        //将匹配权重作为最大化目标函数
        IloNumExpr obj = model.linearNumExpr();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    obj = model.sum(obj, model.prod(match[i][j], validMatrix[i][j]));
                }
            }
        }
        //将带权路径总和作为最大化目标函数

        model.addMaximize(obj);

        //一个司机最多匹配一个乘客
        for (int i = 0; i < nDrivers; i++) {
            IloNumExpr expr = model.linearNumExpr();
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    expr = model.sum(expr, match[i][j]);
                }
            }
            model.addLe(expr, 1);
        }
        //一个乘客最多匹配一个司机
        for (int j = 0; j < nPassengers; j++) {
            IloNumExpr expr = model.linearNumExpr();
            for (int i = 0; i < nDrivers; i++) {
                if (validMatrix[i][j] > 0) {
                    expr = model.sum(expr, match[i][j]);
                }
            }
            model.addLe(expr, 1);
        }

        model.solve();
        matchMatrix = new int[nDrivers][nPassengers];
        Solution solution = new Solution();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    matchMatrix[i][j] = (int) model.getValue(match[i][j]);
                }
                if (matchMatrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.curDriver = driver;
                    Passenger passenger1 = driver.queue.getFirst();
                    Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                    Pattern pattern = new Pattern(driver, passenger1, passenger2);
                    double eta1 = Param.touringMap.calTimeDistance(passenger1.originCoor, driver.curCoor);
                    double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
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
                if (validMatrix[i][j] > 0) {
                    match[i][j] = solver.makeVar(0, 1, false, i + "," + j);
                }
            }
        }
        for (int i = 0; i < nDrivers; i++) {
            MPConstraint d = solver.makeConstraint(0, 1, "d" + i);
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    d.setCoefficient(match[i][j], 1);
                }
            }
        }

        for (int j = 0; j < nPassengers; j++) {
            MPConstraint p = solver.makeConstraint(0, 1, "p" + "j");
            for (int i = 0; i < nDrivers; i++) {
                if (validMatrix[i][j] > 0) {
                    p.setCoefficient(match[i][j], 1);
                }
            }
        }

        MPObjective obj = solver.objective();
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    obj.setCoefficient(match[i][j], validMatrix[i][j]);
                }
            }
        }
        obj.setMaximization();
        solver.solve();
        Solution solution = new Solution();
        matchMatrix = new int[nDrivers][nPassengers];
        for (int i = 0; i < nDrivers; i++) {
            for (int j = 0; j < nPassengers; j++) {
                if (validMatrix[i][j] > 0) {
                    double val = match[i][j].solutionValue();
                    if (Param.equals(val, 1)) {
                        matchMatrix[i][j] = 1;
                    }
                }
                if (matchMatrix[i][j] == 1) {
                    Driver driver = driverList.get(i);
                    Passenger passenger = passengerList.get(j);
                    //           System.out.println(map.calSpatialDistance(driver.cur_coor, passenger.origin_coor));
                    driver.queue.add(passenger);
                    passenger.curDriver = driver;
                    Passenger passenger1 = driver.queue.getFirst();
                    Passenger passenger2 = driver.queue.size() == 2 ? driver.queue.getLast() : null;
                    Pattern pattern = new Pattern(driver, passenger1, passenger2);
                    double eta1 = Param.touringMap.calTimeDistance(passenger1.originCoor, driver.curCoor);
                    double eta2 = passenger2 == null ? Param.MAX_ETA2 : Param.touringMap.calTimeDistance(passenger1.originCoor, passenger2.originCoor);
                    pattern.setAim(passenger2 == null ? 0 : Param.touringMap.calSimilarity(passenger1, passenger2), eta1, eta2);
                    solution.patterns.add(pattern);
                    solution.profit += pattern.aim;
                }
            }
        }
        return solution;
    }

    public int removeInvalid(long curTime) {
        List<Passenger> removePassengers = new ArrayList<>();
        for (Passenger passenger : passengerList) {
            if (curTime - passenger.submitTime >= passenger.expectedArriveTime * Param.LEAVING_COFF) {
                removePassengers.add(passenger);
            }
        }
        passengerList.removeAll(removePassengers);
        return removePassengers.size();
    }

    public void remove(Solution sol, long curTime) {
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
            passenger.renew(curTime);
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
                double etaAim = Param.touringMap.calTimeDistance(driver.curCoor, passenger1.originCoor);
                double sameAim = 0.0;
                double aim = (sameAim > 0 ? (sameAim + 2) : 0) + 2 - etaAim / Param.MAX_ETA;
                profit += aim;
            }
        }
        return profit;
    }
}
