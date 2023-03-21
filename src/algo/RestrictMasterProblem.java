package algo;

import common.Param;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import javafx.util.Pair;
import model.Instance;
import model.Pattern;

import java.util.ArrayList;
import java.util.BitSet;

public class RestrictMasterProblem {
    Instance inst;
    int nPassengers;
    int nDrivers;

    // cplex
    IloCplex cplex;
    IloObjective obj;
    IloRange[] ranges;
    ArrayList<IloNumVar> x;
    IloNumVar[] artificialVars;
    ArrayList<Pattern> pool;

    // diving heuristic
    BitSet fixedItems;

    // param
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

    void formulate() throws IloException {
        cplex = new IloCplex();
        x = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new IloRange[nPassengers + nDrivers];
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

    void addArtificialVariables() throws IloException {
        artificialVars = new IloNumVar[nPassengers + nDrivers];
        // artificial var added in the constraints(1)
        for (int i = 0; i < nDrivers; i++) {
            IloColumn col = cplex.column(obj, bigM);
            col = col.and(cplex.column(ranges[i], 1));
            artificialVars[i] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnDriver" + i);
        }
        // artificial var added in the constraints(2)
        for (int j = 0; j < nPassengers; j++) {
            IloColumn col = cplex.column(obj, bigM);
            col = col.and(cplex.column(ranges[nDrivers + j], 1));
            artificialVars[nDrivers + j] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnPassenger" + j);
        }
    }

    void set() {

    }

    void setDiving() {

    }

    void recoverDiving() {

    }

    void addColumns(ArrayList<Pattern> patterns) throws IloException {
        for (Pattern pattern : patterns) {
            int size = pool.size();
            // add columns and vars
            IloColumn col = cplex.column(obj, pattern.time);
            // range on driver
            col = col.and(cplex.column(ranges[pattern.driverId], 1));
            // range on two passengers
            col = col.and(cplex.column(ranges[pattern.passenger1Id], 1));
            col = col.and(cplex.column(ranges[pattern.passenger2Id], 1));
            // new column var
            String name = "x_" + pattern.driverId + "," + pattern.passenger1Id + "," + pattern.passenger2Id;
            IloNumVar var = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, name);
            pattern.colVar = var;
            x.add(var);
        }
    }

    void solveLP() throws IloException {
        boolean feasible = cplex.solve();
    }

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

    double getObjVal() throws IloException {
        return cplex.getObjValue();
    }

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

    void end() {
        cplex.end();
    }
}
