package algo;

import common.Param;
import javafx.util.Pair;
import model.Instance;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;
import java.util.BitSet;

enum DivingStrategy {
    Fractional, Guided, Aim
}

public class DivingHeuristic {
    Instance inst;
    int nDrivers;
    int nPassengers;

    LPSol lpSol;
    LPSol fixedSol;

    RMP_SCIP rmp;
    RestrictMasterProblem rmpCplex;
    PricingProblem pp;

    // diving param
    int nlpMax = 1000;
    int iterMax = 100;
    int iterMin = 10;

    int boundStep = 10;

    public DivingHeuristic(Instance inst, RMP_SCIP rmp, RestrictMasterProblem rmpCplex, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = rmp;
        this.rmpCplex = rmpCplex;
        this.pp = pp;
    }

    public Solution solve(LPSol lpSol, int ub) {
        this.lpSol = lpSol;

        // set fixedIdx and fracIdx
        Pair<Integer, BitSet[]> fracPair = setFixedAndFracIdx();
        int nFrac = fracPair.getKey();
        BitSet fixedIdx = fracPair.getValue()[0];
        BitSet fracIdx = fracPair.getValue()[1];

        double obj = getObj();
        int nlp = 0;
        int iter = 0;
        int nFracBegin = nFrac;
        ColumnGeneration columnGeneration = new ColumnGeneration(inst, rmp, rmpCplex, pp);
        // diving
        while (nFrac != 0 && obj < ub && ((nlp < nlpMax && iter < iterMax) || iter < iterMin || nFrac < nFracBegin - iter * 0.5)) {
            iter++;
            // select and bound var
            DivingStrategy divingStrategy = DivingStrategy.Aim;
            ArrayList<Integer> boundVarsIdx = selectDivingVar(fracIdx, divingStrategy);
            for (Integer boundVarIdx : boundVarsIdx) {
                fracIdx.clear(boundVarIdx);
                fixedIdx.set(boundVarIdx);
            }
            // solve modified LP; update nlp
            rmp.setDiving(fixedIdx, this.lpSol.vals);
            if (Param.USE_CG) {
                this.lpSol = columnGeneration.cg();
            } else {
                this.lpSol = rmp.solveLP();
            }
            // update fracIdx and nFrac
            fracPair = setFixedAndFracIdx();
            nFrac = fracPair.getKey();
            fixedIdx = fracPair.getValue()[0];
            fracIdx = fracPair.getValue()[1];
            obj = getObj();
        }
        // recover diving
//        rmp.recoverDiving(fixedIdx, lpSol.vals);
        // return sol
        if (nFrac != 0) {
            return null;
        } else {
            return getSol();
        }
    }


    // 挑选潜水策略
    public ArrayList<Integer> selectDivingVar(BitSet fracIdx, DivingStrategy divingStrategy) {
        switch (divingStrategy) {
            case Fractional:
                return fractionalDiving(fracIdx);
            case Aim:
                return aimDiving(fracIdx);
            default:
                return new ArrayList<>();
        }
    }

    // 分数潜水启发式
    public ArrayList<Integer> fractionalDiving(BitSet fracIdx) {
        int minFracH = 0;
        double minFrac = 1.0;
        for (int h = fracIdx.nextSetBit(0); h >= 0; h = fracIdx.nextSetBit(h + 1)) {
            double value = lpSol.vals.get(h).getValue();
            if (Math.abs(value - 1.0) < minFrac) {
                minFrac = Math.abs(value - 1.0);
                minFracH = h;
            }
        }
        return null;
    }

    public ArrayList<Integer> aimDiving(BitSet fracIdx) {
        ArrayList<Integer> boundVarsIdx = new ArrayList<>();
        BitSet driverBit = new BitSet(nDrivers);
        BitSet passengerBit = new BitSet(nPassengers);
        boolean find = true;
        while (boundVarsIdx.size() < boundStep && find) {
            find = false;
            int maxAimIdx = -1;
            double maxAim = 0.0;
            for (int h = fracIdx.nextSetBit(0); h >= 0; h = fracIdx.nextSetBit(h + 1)) {
                double value = lpSol.vals.get(h).getValue();
                Pattern pattern = lpSol.vals.get(h).getKey();
                double aim = value * pattern.aim;
                int driverIdx = pattern.driverIdx;
                int passenger1Idx = pattern.passenger1Idx;
                int passenger2Idx = pattern.passenger2Idx;
                boolean driverAvailable = !driverBit.get(driverIdx);
                boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
                boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
                if (aim > maxAim && driverAvailable && passenger1Available && passenger2Available) {
                    maxAim = aim;
                    maxAimIdx = h;
                    find = true;
                }
            }
            if (maxAimIdx >= 0) {
                boundVarsIdx.add(maxAimIdx);
                // update bit
                Pattern pattern = lpSol.vals.get(maxAimIdx).getKey();
                driverBit.set(pattern.driverIdx);
                if (pattern.passenger1Idx > 0) {
                    passengerBit.set(pattern.passenger1Idx);
                }
                if (pattern.passenger2Idx > 0) {
                    passengerBit.set(pattern.passenger2Idx);
                }
            }
        }
        return boundVarsIdx;
    }

    public Pair<Integer, BitSet[]> setFixedAndFracIdx() {
        int nFrac = 0;
        ArrayList<Pair<Pattern, Double>> pairs = lpSol.vals;
        int sizeK = pairs.size();
        BitSet fixedIdx = new BitSet(sizeK);
        BitSet fracIdx = new BitSet(sizeK);
        for (int h = 0; h < sizeK; h++) {
            if (Param.isInt(pairs.get(h).getValue())) {
                fixedIdx.set(h);
            } else {
                fracIdx.set(h);
                nFrac++;
            }
        }
        return new Pair<>(nFrac, new BitSet[]{fixedIdx, fracIdx});
    }

    public double getObj() {
        return lpSol.objVal;
    }

    // 获取可行解
    public Solution getSol() {
        ArrayList<Pattern> patterns = new ArrayList<>();
        double cost = 0;
        for (int h = 0; h < lpSol.vals.size(); h++) {
            patterns.add(lpSol.vals.get(h).getKey());
            cost += lpSol.vals.get(h).getKey().aim;
        }
        return new Solution(patterns, cost);
    }
}
