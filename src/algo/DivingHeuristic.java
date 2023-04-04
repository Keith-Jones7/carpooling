package algo;

import common.Param;
import javafx.util.Pair;
import model.*;
import java.util.*;

enum DivingStrategy {
    Fractional, Guided
}
public class DivingHeuristic {
    Instance inst;
    int nDrivers;
    int nPassengers;

    LPSol lpSol;
    LPSol fixedSol;

    RMP_SCIP rmp;

    // diving param
    int nlpMax = 1000;
    int iterMax = 100;
    int iterMin = 10;

    public DivingHeuristic(Instance inst, RMP_SCIP rmp, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = rmp;
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
        // diving
        while (nFrac != 0 && obj < ub && ((nlp < nlpMax && iter < iterMax) || iter < iterMin || nFrac < nFracBegin - iter*0.5)) {
            iter++;
            // select and bound var
            DivingStrategy divingStrategy = DivingStrategy.Fractional;
            int boundVarIdx = selectDivingVar(fracIdx, divingStrategy);
            fracIdx.clear(boundVarIdx);
            fixedIdx.set(boundVarIdx);
            // solve modified LP; update nlp
            rmp.setDiving(fixedIdx, this.lpSol.vals);
            this.lpSol = rmp.solveLP();
            // update fracIdx and nFrac
            fracPair = setFixedAndFracIdx();
            nFrac = fracPair.getKey();
            fixedIdx = fracPair.getValue()[0];
            fracIdx = fracPair.getValue()[1];
            obj = getObj();
        }
        //System.out.println(iter);
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
    public int selectDivingVar(BitSet fracIdx, DivingStrategy divingStrategy) {
        switch (divingStrategy) {
            case Fractional :
                return fractionalDiving(fracIdx);
            default:
                return -1;
        }
    }

    // 分数潜水启发式
    public int fractionalDiving(BitSet fracIdx) {
        int minFracH = 0;
        double minFrac = 1.0;
        for (int h = fracIdx.nextSetBit(0); h >= 0; h = fracIdx.nextSetBit(h+1)) {
            double value = lpSol.vals.get(h).getValue();
            if (Math.abs(value - 1.0) < minFrac) {
                minFrac = Math.abs(value - 1.0);
                minFracH = h;
            }
        }
        return minFracH;
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
