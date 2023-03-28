package algo;

import ilog.concert.IloException;
import model.Instance;
import model.Pattern;

import java.util.ArrayList;
import java.util.BitSet;

public class ColumnGeneration {
    // instance
    Instance inst;
    int nDrivers;
    int nPassengers;

    RestrictMasterProblem rmp;
    PricingProblem pp;
    public ColumnGeneration(Instance inst, RestrictMasterProblem rmp, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.driverList.size();
        this.nPassengers = inst.passengerList.size();
        this.rmp = rmp;
        this.pp = pp;
    }

    void solve(ArrayList<Pattern> pool, boolean isSolveAll, boolean isSolveLP) {
        try {
            rmp.addColumns(pool);
            if (isSolveAll) {
                solveAll(isSolveLP);
            } else {
                solve();
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    void solveAll(boolean isSolveLP) {
        try {
            if (isSolveLP) {
                rmp.solveLP();
            } else {
                rmp.solveIP();
            }

        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    void solve() {
        try {
            rmp.set();
            pp.set();
            cg();
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    void cg() throws IloException {
        double[] dualsOfRanges;
        BitSet fixedItems;
        double obj;
        rmp.solveLP();
        obj = rmp.getObjVal();
        dualsOfRanges = rmp.getDualsOfRanges();
        fixedItems = rmp.fixedItems;

        pp.solve(dualsOfRanges, fixedItems);
        while (pp.findNewColumns()) {
            rmp.addColumns(pp.patterns);
            rmp.solveLP();
            obj = rmp.getObjVal();
            dualsOfRanges = rmp.getDualsOfRanges();
            pp.solve(dualsOfRanges, fixedItems);
        }

    }

    void end() {
        rmp.cplex.end();
    }
}
