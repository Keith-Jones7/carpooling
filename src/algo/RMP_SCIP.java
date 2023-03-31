package algo;

import common.Param;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import jscip.*;
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

    // scip
    MPSolver solver;
    MPObjective obj;
    MPConstraint[] ranges;
    ArrayList<MPVariable> x;
    ArrayList<Pattern> pool;

    // diving heuristic
    BitSet fixedItems;

    // final param
    private final int intMax = Integer.MAX_VALUE;
    private final double infinity = Double.MAX_VALUE;
    private final double bigM = 1e8;

    public RMP_SCIP (Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        formulate();
    }

    void formulate() {
        // init
        solver = MPSolver.createSolver("SCIP");
        x = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new MPConstraint[nDrivers + nPassengers];
        // objective
        obj = solver.objective();
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

    void addColumns(ArrayList<Pattern> patterns) throws IloException {
        for (Pattern pattern : patterns) {
            pool.add(pattern);
            // add var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            MPVariable var = solver.makeVar(0, 1, true, name);
            // set obj
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

    void solveLP() {
        solver.solve();
    }

    // 求解整数规划
    Sol solveIP() {
        // solve
        solver.solve();
        Sol sol = getIPSol();
        end();
        return sol;
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

    // 获取整数规划解
    Sol getIPSol() {
        double objVal = obj.value();
        Sol sol = new Sol();
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
