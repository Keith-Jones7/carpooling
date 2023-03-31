package algo;

import common.Param;
import javafx.util.Pair;
import jscip.Variable;
import model.*;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;


import java.util.*;

public class RMP_SCIP {
    // instance
    Instance inst;
    int nPassengers;
    int nDrivers;

    // ortools
    MPSolver solver;
    MPObjective obj;
    MPConstraint[] ranges;
    ArrayList<MPVariable> x;
    ArrayList<Pattern> pool;

    // diving heuristic
    BitSet fixedDrivers;
    BitSet fixedPassengers;

    // final param
    private final int intMax = Integer.MAX_VALUE;
    private final double infinity = Double.MAX_VALUE;
    private final double bigM = 1e8;

    public RMP_SCIP (Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        this.fixedDrivers = new BitSet(nDrivers);
        this.fixedPassengers = new BitSet(nPassengers);
        formulate();
    }

    void formulate() {
        // init
        Loader.loadNativeLibraries();
        solver = MPSolver.createSolver("SCIP");
        x = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new MPConstraint[nDrivers + nPassengers];
        // objective
        obj = solver.objective();
        obj.setMaximization();
//        // add variables
//        for (int p = 0; p < pool.size(); p++) {
//            Pattern pattern = pool.get(p);
//            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
//            MPVariable var = solver.makeIntVar(0, 1, name);
//            pattern.var = var;
//            x.add(var);
//        }
        // constraints
        // (1): the driver constraint
        for (int i = 0; i < nDrivers; i++) {
            ranges[i] = solver.makeConstraint(0, 1, "rangeOnDriver" + i);
        }
        // (2): the passenger constraint
        for (int j = 0; j < nPassengers; j++) {
            ranges[nDrivers + j] = solver.makeConstraint(0, 1, "rangeOnPassenger" + j);
        }

    }

    public void set() {

    }

    public void setDiving(BitSet fixedIdx, ArrayList<Pair<Pattern, Double>> vals) {
            for (int h = fixedIdx.nextSetBit(0); h >= 0; h = fixedIdx.nextSetBit(h+1)) {
                Pair<Pattern, Double> val = vals.get(h);
                MPVariable var = val.getKey().var;
                var.setBounds(1.0, 1.0);
                // fix drivers and passengers
                fixedDrivers.set(val.getKey().driverIdx);
                if (val.getKey().passenger1Idx >= 0) {
                    fixedPassengers.set(val.getKey().passenger1Idx);
                }
                if (val.getKey().passenger2Idx >= 0) {
                    fixedPassengers.set(val.getKey().passenger2Idx);
                }
            }
    }

    public void recoverDiving(BitSet fixedIdx, ArrayList<Pair<Pattern, Double>> vals) {
        fixedDrivers = new BitSet(nDrivers);
        fixedPassengers = new BitSet(nPassengers);
        for (int h = fixedIdx.nextSetBit(0); h >= 0; h = fixedIdx.nextSetBit(h+1)) {
            Pair<Pattern, Double> val = vals.get(h);
            MPVariable var = val.getKey().var;
            var.setBounds(0, infinity);
        }
    }

    void addColumns(ArrayList<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            pool.add(pattern);
            // add var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            MPVariable var = solver.makeVar(0, 1, false, name);
            // set obj
            obj.setCoefficient(var, pattern.aim);
            // set range on driver
            ranges[pattern.driverIdx].setCoefficient(var, 1);
            // set range on two passengers
            if (pattern.passenger1Id >= 0) {
                ranges[nDrivers + pattern.passenger1Idx].setCoefficient(var, 1);
            }
            if (pattern.passenger2Id >= 0) {
                ranges[nDrivers + pattern.passenger2Idx].setCoefficient(var, 1);
            }
            pattern.var = var;
            x.add(var);
        }
    }

    LPSol solveLP() {
        solver.solve();
        return getLPSol();
    }

    // 求解整数规划
    Solution solveIP() {
        // solve
        convertToIP();
        solver.solve();
        return getIPSol();
    }

    // 转变为整数规划
    private void convertToIP() {
        for (MPVariable var : x) {
            var.setInteger(true);
        }
    }

    // 转变为线性规划
    private void convertToLP() {
        for (MPVariable var : x) {
            var.setInteger(false);
        }
    }

    // 获取对偶变量
    double[] getDualsOfRanges() {
        double[] dualsOfRanges = new double[nDrivers + nPassengers];
        for (int i = 0; i < nDrivers + nPassengers; i++) {
            dualsOfRanges[i] = ranges[i].dualValue();
        }
        return dualsOfRanges;
    }

    // 获取最优目标值
    double getObjVal() {
        return obj.value();
    }

    // 获取线性规划解
    LPSol getLPSol() {
        double objVal = obj.value();
        ArrayList<Pair<Pattern, Double>> vals = new ArrayList<>();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = x.get(p).solutionValue();
            if (val > Param.EPS) {
                vals.add(new Pair<>(pattern, val));
            }
        }
        return new LPSol(vals, objVal);
    }

    // 获取整数规划解
    Solution getIPSol() {
        double objVal = obj.value();
        Solution sol = new Solution();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = x.get(p).solutionValue();
            if (Param.equals(val, 1)) {
                sol.patterns.add(pattern);
            }
        }
        sol.profit = objVal;
        return sol;
    }

    // 求解结束
    void end() {
        solver.clear();
    }
}
