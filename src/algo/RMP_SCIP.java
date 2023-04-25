package algo;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import javafx.util.Pair;
import model.Instance;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;
import java.util.BitSet;

public class RMP_SCIP {
    // final param
    private final int intMax = Integer.MAX_VALUE;
    private final double infinity = 1e8;
    private final double bigM = 1e8;
    // instance
    Instance inst;
    int nPassengers;
    int nDrivers;
    // ortools
    MPSolver solver;
    MPObjective obj;
    MPConstraint[] ranges;
    ArrayList<MPVariable> x;
    MPVariable[] artificialVars;
    ArrayList<Pattern> pool;
    // diving heuristic
    BitSet fixedDrivers;
    BitSet fixedPassengers;

    public RMP_SCIP(Instance inst) {
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
        String solverName = "GLOP";
        solver = MPSolver.createSolver(solverName);

        x = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new MPConstraint[nDrivers + nPassengers];
        // objective
        obj = solver.objective();
        obj.setMaximization();
        // constraints
        // (1): the driver constraint
        for (int i = 0; i < nDrivers; i++) {
            ranges[i] = solver.makeConstraint(0, 1, "rangeOnDriver" + i);
        }
        // (2): the passenger constraint
        for (int j = 0; j < nPassengers; j++) {
            ranges[nDrivers + j] = solver.makeConstraint(0, 1, "rangeOnPassenger" + j);
        }

        addArtificialVariable();
    }

    void addArtificialVariable() {
        artificialVars = new MPVariable[nDrivers + nPassengers];
        // artificial var added in the constraints(1)
        for (int i = 0; i < nDrivers; i++) {
            MPVariable var = solver.makeVar(0, infinity, false, "avOnDriver" + i);
            obj.setCoefficient(var, -bigM);
            ranges[i].setCoefficient(var, 1);
            artificialVars[i] = var;
        }
        // artificial var added in the constraints(2)
        for (int j = 0; j < nPassengers; j++) {
            MPVariable var = solver.makeVar(0, infinity, false, "avOnPassenger" + j);
            obj.setCoefficient(var, -bigM);
            ranges[nDrivers + j].setCoefficient(var, 1);
            artificialVars[nDrivers + j] = var;
        }
    }

    public void set() {

    }

    public void setDiving(BitSet fixedIdx, ArrayList<Pair<Pattern, Double>> vals) {
        for (int h = fixedIdx.nextSetBit(0); h >= 0; h = fixedIdx.nextSetBit(h + 1)) {
            Pair<Pattern, Double> val = vals.get(h);
            MPVariable var = val.getKey().var;
            var.setBounds(1.0, 1.0 + Param.EPS);
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
        for (int h = fixedIdx.nextSetBit(0); h >= 0; h = fixedIdx.nextSetBit(h + 1)) {
            Pair<Pattern, Double> val = vals.get(h);
            MPVariable var = val.getKey().var;
            var.setBounds(0, infinity);
        }
    }

    void addColumns(ArrayList<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            // 测试：不能在pool中加入重复pattern
            if (pool.contains(pattern)) {
                System.out.println("error: pattern " + pattern.toString() + "already in pool");
            }
            pool.add(pattern);
            // add var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            MPVariable var = solver.makeVar(0, infinity, false, name);
            // set obj
            obj.setCoefficient(var, pattern.aim);
            // set range on driver
            ranges[pattern.driverIdx].setCoefficient(var, 1);
            // set range on two passengers
            if (pattern.passenger1Idx >= 0) {
                ranges[nDrivers + pattern.passenger1Idx].setCoefficient(var, 1);
            }
            if (pattern.passenger2Idx >= 0) {
                ranges[nDrivers + pattern.passenger2Idx].setCoefficient(var, 1);
            }
            pattern.var = var;
            x.add(var);
        }
    }

    LPSol solveLP() {
//        MPSolverParameters parameters = new MPSolverParameters();
//        parameters.setIntegerParam(MPSolverParameters.IntegerParam.LP_ALGORITHM, 10);
        solver.solve();
        return getLPSol();
    }

    // 求解整数规划
    Solution solveIP() {
//        MPSolverParameters parameters = new MPSolverParameters();
//        parameters.setIntegerParam(MPSolverParameters.IntegerParam.LP_ALGORITHM, 11);
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

    // model feasible only when all artificial vars are 0
    boolean isModelFeasible() {
        // check artificial on drivers
        for (int i = 0; i < nDrivers; i++) {
            double val = artificialVars[i].solutionValue();
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        // check artificial on passengers
        for (int j = 0; j < nPassengers; j++) {
            double val = artificialVars[nDrivers + j].solutionValue();
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        return true;
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
