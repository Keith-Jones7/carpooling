package algo;

import common.Param;
import ilog.concert.IloException;
import model.Instance;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;
import java.util.BitSet;

public class BranchAndBound {

    public Instance inst;
    public int nDrivers;
    public int nPassengers;
    public Solution bestSol;

    public RestrictMasterProblem rmp;
    public PricingProblem pp;
    public ColumnGeneration cg;
    public BranchAndBound(Instance inst) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = new RestrictMasterProblem(inst);
        this.pp = new PricingProblem(inst);
        this.cg = new ColumnGeneration(inst, rmp, pp);
        this.bestSol = new Solution();
    }

    public void run() {
        try {
            bestSol = genMIPSol();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    private void solve() {
//        ArrayList<Pattern> pool = genInitPatterns();
        ArrayList<Pattern> pool = genAllPatterns();
        boolean isSolveAll = true;
        boolean isSolveLP = false;
        cg.solve(pool, isSolveAll, isSolveLP);
    }

    public void updateUB() {

    }

    public void updateLB() {

    }

    public void createRoot() {

    }

    Solution genMIPSol() throws IloException {
        ArrayList<Pattern> pool = genAllPatterns();
        rmp.addColumns(pool);
        return rmp.solveIP();
    }

    public ArrayList<Pattern> genInitPatterns() {
        ArrayList<Pattern> pool = new ArrayList<>();

        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        boolean remainPattern = true;
        while (remainPattern) {
            remainPattern = false;
            double maxAim = -Double.MAX_VALUE;
            int driverId = -1;
            int passenger1Id = -1;
            int passenger2Id = -1;
            double sameTime = 0;
            double getTime = 0;
            // 遍历所有顾客对
            for (int i = 0; i < nPassengers; i++) {
                for (int j = 0; j < nPassengers; j++) {
                    // 当两个顾客尚未被服务，且满足拼车条件时
                    if (i != j && inst.ppValidMatrix[i][j] > 0 && !passengerBit.get(i) && !passengerBit.get(j)) {
                        for (int k = 0; k < nDrivers; k++) {
                            if (!driverBit.get(k)) {
                                double aim = Param.obj1Coef * inst.ppValidMatrix[i][j] - inst.dpTimeMatrix[k][i];
                                if (aim > maxAim) {
                                    maxAim = aim;
                                    driverId = k;
                                    passenger1Id = i;
                                    passenger2Id = j;
                                    remainPattern = true;
                                }
                            }
                        }
                    }
                }
            }
            if (remainPattern) {
                driverBit.set(driverId);
                passengerBit.set(passenger1Id);
                passengerBit.set(passenger2Id);

                Pattern pattern = new Pattern(sameTime, getTime, driverId, passenger1Id, passenger2Id);
                pool.add(pattern);
            }
        }
        return pool;
    }

    // 生成所有pattern
    public ArrayList<Pattern> genAllPatterns() {
        ArrayList<Pattern> pool = new ArrayList<>();
        for (int j1 = 0; j1 < nPassengers; j1++) {
            for (int j2 = 0; j2 < nPassengers; j2++) {
                double sameTime = inst.ppValidMatrix[j1][j2];
                if (j1 != j2 && sameTime > 0) {
                    for (int i = 0; i < nDrivers; i++) {
                        double getTime = inst.dpTimeMatrix[i][j1] + inst.ppTimeMatrix[j1][j2];
                        if (getTime < Param.MAX_ETA) {
                            Pattern pattern = new Pattern(i, j1, j2);
                            pattern.setTime(sameTime, getTime);
                            pool.add(pattern);
                        }
                    }
                }
            }
        }
        return pool;
    }

    public void branch() {

    }
}
