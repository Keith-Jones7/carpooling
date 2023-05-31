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
    /**
     * 问题信息
     */
    Instance inst; // 问题实例
    int nDrivers;  // 司机数量
    int nPassengers;  // 乘客数量

    LPSol lpSol;  // 松弛解

    /**
     * 求解模块
     */
    RMP_SCIP rmp;  // 主问题求解模块
    PricingProblem pp; // 子问题求解模块

    /**
     * 潜水启发式控制参数
     */
    int iterMax = 1000;   // 最大迭代次数
    int iterMin = 10;    // 最小迭代次数
    int boundStep = 10;   // 潜水步长

    /**
     * 构造函数
     * @param inst
     * @param rmp
     * @param pp
     */
    public DivingHeuristic(Instance inst, RMP_SCIP rmp, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = rmp;
        this.pp = pp;
    }

    /**
     * 潜水启发式求解函数
     * @param lpSol 松弛解
     * @param ub 上界
     * @return 整数可行解
     */
    public Solution solve(LPSol lpSol, int ub) {
        this.lpSol = lpSol;

        // set fixedIdx and fracIdx
        Pair<Integer, BitSet[]> fracPair = setFixedAndFracIdx();
        int nFrac = fracPair.getKey();
        BitSet fixedIdx = fracPair.getValue()[0];
        BitSet fracIdx = fracPair.getValue()[1];

        double obj = getObj();
        int iter = 0;
        int nFracBegin = nFrac;
        ColumnGeneration columnGeneration = new ColumnGeneration(inst, rmp, null, pp);
        // diving
        while (nFrac != 0 && obj < ub && (iter < iterMax || iter < iterMin || nFrac < nFracBegin - iter * 0.5)) {
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
            this.lpSol = columnGeneration.cg();
            // update fracIdx and nFrac
            fracPair = setFixedAndFracIdx();
            nFrac = fracPair.getKey();
            fixedIdx = fracPair.getValue()[0];
            fracIdx = fracPair.getValue()[1];
            obj = getObj();
        }
        // return sol
        if (nFrac != 0) {
            return null;
        } else {
            return getSol();
        }
    }


    /**
     * 挑选分数潜水变量
     * @param fracIdx 分数变量索引
     * @param divingStrategy 潜水策略
     * @return 返回待潜水变量的索引
     */
    public ArrayList<Integer> selectDivingVar(BitSet fracIdx, DivingStrategy divingStrategy) {
        switch (divingStrategy) {
            case Aim:
                return aimDiving(fracIdx);
            default:
                return new ArrayList<>();
        }
    }

    /**
     * 目标值导向潜水
     * @param fracIdx 分数变量索引
     * @return 返回待潜水变量的索引
     */
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

    /**
     * 寻找整数和分数变量
     * @return 返回一个二元组，第一个元素表示分数变量的数目，第二个元素是整数变量索引与分数变量索引数组
     */
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

    /**
     * 获取松弛解的目标值
     * @return 返回松弛解的目标值
     */
    public double getObj() {
        return lpSol.objVal;
    }

    /**
     * 获取可行解
     * @return 将松弛解LpSol转变为Solution
     */
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
