package algo;

import common.Param;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import javafx.util.Pair;
import model.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class RestrictMasterProblem {
    // instance
    Instance inst;
    int nPassengers;
    int nDrivers;

    // cplex
    IloCplex cplex;
    IloObjective obj;
    IloRange[] ranges;
    ArrayList<IloNumVar> x;
    ArrayList<IloConversion> x_conv;
    IloNumVar[] artificialVars;
    ArrayList<Pattern> pool;

    // diving heuristic
    BitSet fixedItems;

    // final param
    private final int IloIntMax = Integer.MAX_VALUE;
    private final double IloInfinity = Double.MAX_VALUE;
    private final double bigM = 1e8;

    public RestrictMasterProblem(Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        try {
            formulate();
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    // 建立模型
    void formulate() throws IloException {
        cplex = new IloCplex();
        x = new ArrayList<>();
        x_conv = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new IloRange[nDrivers + nPassengers];
        // objective
        obj = cplex.addMaximize();

        // constraints
        // (1): the driver constraint
        for (int i = 0; i < nDrivers; i++) {
            ranges[i] = cplex.addRange(0, 1, "rangeOnDriver" + i);
        }
        // (2): the passenger constraint
        for (int j = 0; j < nPassengers; j++) {
            ranges[nDrivers + j] = cplex.addRange(0, 1, "rangeOnPassenger" + j);
        }
        addArtificialVariables();

        cplex.setOut(null);
        cplex.setWarning(null);
        cplex.setParam(IloCplex.IntParam.RandomSeed, Param.SEED);
        cplex.setParam(IloCplex.IntParam.RootAlgorithm, IloCplex.Algorithm.Primal);
    }

    // 添加虚拟变量
    void addArtificialVariables() throws IloException {
        artificialVars = new IloNumVar[nDrivers + nPassengers];
        // artificial var added in the constraints(1)
        for (int i = 0; i < nDrivers; i++) {
            IloColumn col = cplex.column(obj, -bigM);
            col = col.and(cplex.column(ranges[i], 1));
            artificialVars[i] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnDriver" + i);
        }
        // artificial var added in the constraints(2)
        for (int j = 0; j < nPassengers; j++) {
            IloColumn col = cplex.column(obj, -bigM);
            col = col.and(cplex.column(ranges[nDrivers + j], 1));
            artificialVars[nDrivers + j] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnPassenger" + j);
        }
    }

    // 设置属性
    void set() {

    }

    // 设置潜水启发式
    void setDiving() {

    }

    // 移除潜水启发式
    void recoverDiving() {

    }

    // 添加列变量
    void addColumns(ArrayList<Pattern> patterns) throws IloException {
        for (Pattern pattern : patterns) {
            int size = pool.size();
            pool.add(pattern);
            // add columns and vars
            IloColumn col = cplex.column(obj, pattern.aim);
            // range on driver
            col = col.and(cplex.column(ranges[pattern.driverIdx], 1));
            // range on two passengers
            if (pattern.passenger1Id >= 0) {
                col = col.and(cplex.column(ranges[nDrivers + pattern.passenger1Idx], 1));
            }
            if (pattern.passenger2Id >= 0) {
                col = col.and(cplex.column(ranges[nDrivers + pattern.passenger2Idx], 1));
            }
            // new column var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            IloNumVar var = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, name);
            pattern.colVar = var;
            x.add(var);
        }
    }

    void removeInvalidRanges(ArrayList<Pattern> allPatterns) throws IloException {
        int[] validRangesIdx = new int[nDrivers + nPassengers];
        IloRange[] removeRanges = new IloRange[nDrivers + nPassengers];
        for (Pattern pattern : allPatterns) {
            if (pattern.driverIdx >= 0) {
                validRangesIdx[pattern.driverIdx] = 1;
            }
            if (pattern.passenger1Idx >= 0) {
                validRangesIdx[nDrivers + pattern.passenger1Idx] = 1;
            }
            if (pattern.passenger2Idx >= 0) {
                validRangesIdx[nDrivers + pattern.passenger2Idx] = 1;
            }
        }
        for (int i = 0; i < nDrivers + nPassengers; i++) {
            if (validRangesIdx[i] == 0) {
                removeRanges[i] = ranges[i];
            }
        }
        cplex.remove(removeRanges);
    }

    // 求解线性规划
    void solveLP() throws IloException {
        boolean feasible = cplex.solve();
    }

    // 求解整数规划
    Solution solveIP() throws IloException {
        Solution sol = null;
        convertToIP();
        cplex.solve();
        boolean feasible = isModelFeasible();
        if (feasible) {
            sol = getIPSol();
        }
        convertToLP();
        return sol;
    }

    // 转变为线性规划
    private void convertToIP() {
        try {
            for (IloNumVar iloNumVar : x) {
                IloConversion conv = cplex.conversion(iloNumVar, IloNumVarType.Int);
                cplex.add(conv);
                x_conv.add(conv);
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    // 转变为整数规划
    private void convertToLP() {
        try {
            for (IloConversion iloConversion : x_conv) {
                cplex.remove(iloConversion);
            }
            x_conv.clear();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    // 获取对偶变量
    double[] getDualsOfRanges() throws IloException {
        return cplex.getDuals(ranges);
    }

    // model feasible only when all artificial vars are 0
    boolean isModelFeasible() throws IloException {
        // check artificial on drivers
        for (int i = 0; i < nDrivers; i++) {
            double val = cplex.getValue(artificialVars[i]);
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        // check artificial on passengers
        for (int j = 0; j < nPassengers; j++) {
            double val = cplex.getValue(artificialVars[nDrivers + j]);
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        return true;
    }

    // 获取最优目标值
    double getObjVal() throws IloException {
        return cplex.getObjValue();
    }

    // 获取线性规划解
    LPSol getLPSol() throws IloException {
        double objVal = cplex.getObjValue();
        ArrayList<Pair<Pattern, Double>> vals = new ArrayList<>();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = cplex.getValue(x.get(p));
            if (val > Param.EPS) {
                vals.add(new Pair<>(pattern, val));
            }
        }
        return new LPSol(vals, objVal);
    }

    // 获取整数规划解
    Solution getIPSol() throws IloException {
        double objVal = cplex.getObjValue();
        Solution sol = new Solution();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = cplex.getValue(x.get(p));
            if (Param.equals(val, 1)) {
                sol.patterns.add(pattern);
            }
        }
        sol.profit = objVal;
        return sol;
    }

    // 求解结束
    void end() {
        cplex.end();
    }
}
