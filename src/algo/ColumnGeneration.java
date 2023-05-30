package algo;

import common.Param;
import model.Instance;
import model.Pattern;

import java.util.ArrayList;
import java.util.BitSet;

public class ColumnGeneration {
    // instance
    Instance inst;
    int nDrivers;
    int nPassengers;


    RMP_SCIP rmp;
    RestrictMasterProblem rmpCplex;
    PricingProblem pp;

    public ColumnGeneration(Instance inst, RMP_SCIP rmp, RestrictMasterProblem rmpCplex, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.driverList.size();
        this.nPassengers = inst.passengerList.size();
        this.rmp = rmp;
        this.rmpCplex = rmpCplex;
        this.pp = pp;
    }

    LPSol solve(ArrayList<Pattern> pool, ArrayList<Pattern> totalPool) {
        rmp.addColumns(pool);
        return solve(totalPool);
    }

    LPSol solve(ArrayList<Pattern> totalPool) {
        pp.set(totalPool);
        return cg();
    }

    LPSol cg() {
        double[] dualsOfRanges;
        BitSet fixedDrivers;
        BitSet fixedPassengers;
        double obj;
        long s0 = System.currentTimeMillis();
        rmp.solveLP();
        double timeCost = Param.getTimeCost(s0);
        obj = rmp.getObjVal();
        dualsOfRanges = rmp.getDualsOfRanges();
        fixedDrivers = rmp.fixedDrivers;
        fixedPassengers = rmp.fixedPassengers;

        pp.solve(dualsOfRanges, fixedDrivers, fixedPassengers);
        while (pp.findNewColumns()) {
            rmp.addColumns(pp.patterns);
            s0 = System.currentTimeMillis();
            rmp.solveLP();
            timeCost = Param.getTimeCost(s0);
            obj = rmp.getObjVal();
            dualsOfRanges = rmp.getDualsOfRanges();
            pp.solve(dualsOfRanges, fixedDrivers, fixedPassengers);
        }
        return rmp.getLPSol();
    }

    void end() {
        rmp.end();
    }
}
