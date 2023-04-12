package algo;

import common.Param;
import ilog.concert.IloException;
import model.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;

public class BranchAndBound {

    public Instance inst;
    public int nDrivers;
    public int nPassengers;
    public Solution bestSol;

    public RMP_SCIP rmp;
    public PricingProblem pp;
    public DivingHeuristic divingHeur;
    public ColumnGeneration cg;
    public BranchAndBound(Instance inst) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = new RMP_SCIP(inst);
        this.pp = new PricingProblem(inst);
        this.divingHeur = new DivingHeuristic(inst, rmp, pp);
        this.cg = new ColumnGeneration(inst, rmp, pp);
        this.bestSol = new Solution();
    }

    public void run() {
        if (Param.USE_CG) {
            bestSol = solve();
        } else {
            bestSol = genMIPSol();
        }

    }

    private Solution solve() {
//        ArrayList<Pattern> pool = genInitPatterns();
        //ArrayList<Pattern> pool = genAllPatterns();
        ArrayList<Pattern> totalPool = genAllPatterns();
        // CG求解LP
        if (Param.LP_IP) {
            ArrayList<Pattern> pool = genInitPatternsGreedy(totalPool);
//            ArrayList<Pattern> pool = new ArrayList<>();
//            totalPool.removeAll(pool);
            long s0 = System.currentTimeMillis();
            LPSol lpSol = cg.solve(pool, totalPool);
            double timeCost = Param.getTimecost(s0);
            return divingHeur.solve(lpSol, Integer.MAX_VALUE);
        }
        // 求解IP
        else {
            rmp.addColumns(totalPool);
            return rmp.solveIP();
        }
    }

    public void updateUB() {

    }

    public void updateLB() {

    }

    public void createRoot() {

    }

    Solution genMIPSol() {
        ArrayList<Pattern> pool = genAllPatterns();
        rmp.addColumns(pool);
        if (Param.LP_IP) {
            long s0 = System.currentTimeMillis();
            LPSol lpSol = rmp.solveLP();
            double timeCost = Param.getTimecost(s0);
            return divingHeur.solve(lpSol, Integer.MAX_VALUE);
        } else {
            return rmp.solveIP();
        }
    }

    public ArrayList<Pattern> genInitPatternsGreedy(ArrayList<Pattern> totalPool) {
        ArrayList<Pattern> pool = new ArrayList<>();

        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        BitSet invalidPatternBit = new BitSet(totalPool.size());
        boolean remainPattern = true;
        while (remainPattern) {
            remainPattern = false;
            // 遍历totalPool，找到最大aim的pool
            double maxAim = 0;
            int maxAimIdx = -1;
            for (int i = 0; i < totalPool.size(); i++) {
                if (!invalidPatternBit.get(i)) {
                    Pattern pattern = totalPool.get(i);
                    int driverIdx = pattern.driverIdx;
                    int passenger1Idx = pattern.passenger1Idx;
                    int passenger2Idx = pattern.passenger2Idx;
                    boolean driverAvailable = !driverBit.get(driverIdx);
                    boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
                    boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
                    if (driverAvailable && passenger1Available && passenger2Available) {
                        // 与最大aim比较
                        if (pattern.aim > maxAim) {
                            maxAim = pattern.aim;
                            maxAimIdx = i;
                        }
                    } else {
                        invalidPatternBit.set(i);
                    }
                }
            }
            // 找到一个可行最大aim的pattern后，将其放入pool中，并更新bit
            if (maxAimIdx >= 0) {
                remainPattern = true;
                Pattern pattern = totalPool.get(maxAimIdx);
                driverBit.set(pattern.driverIdx);
                if (pattern.passenger1Idx >= 0) {
                    passengerBit.set(pattern.passenger1Idx);
                }
                if (pattern.passenger2Idx >= 0) {
                    passengerBit.set(pattern.passenger2Idx);
                }
                invalidPatternBit.set(maxAimIdx);
                pool.add(pattern);
            }
        }
        return pool;
    }

    // 生成所有pattern
    public ArrayList<Pattern> genAllPatterns() {

        ArrayList<Pattern> pool = new ArrayList<>();
        for (int i = 0; i < nDrivers; i++) {
            // 若司机尚未接客，则该司机可以接一个拼车方案或者只接一个乘客
            if (inst.driverList.get(i).queue.size() == 0) {
                // 先遍历第一个乘客，如果满足eta约束，则直接生成pattern放入pool中
                for (int j1 = 0; j1 < nPassengers; j1++) {
                    double etaAim1 = inst.dpTimeMatrix[i][j1];
                    if (etaAim1 <= Param.MAX_ETA) {
                        // 生成一个司机只带一个顾客的方案
                        Pattern pattern1 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), null);
                        pattern1.setAim(0.0, etaAim1, Param.MAX_ETA2);
                        pattern1.setIdx(i, j1, -1);
                        pattern1.setCur_time(inst.cur_time);
                        pool.add(pattern1);
                        // 遍历第二个乘客，如果满足绕行约束和eta约束，则生成拼车pattern放入pool中
                        if (inst.match_flag >= 2) {
                            for (int j2 = 0; j2 < nPassengers; j2++) {
                                if (j2 == j1) {
                                    continue;
                                }
                                double etaAim2 = inst.ppTimeMatrix[j1][j2];
                                double sameAim = inst.ppValidMatrix[j1][j2];
                                if (etaAim2 <= Param.MAX_ETA2 && sameAim > 0) {
                                    // 生成一个司机带两个乘客的拼车方案
                                    Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), inst.passengerList.get(j2));
                                    pattern2.setAim(sameAim, etaAim1, etaAim2);
                                    pattern2.setIdx(i, j1, j2);
                                    pattern2.setCur_time(inst.cur_time);
                                    pool.add(pattern2);
                                }
                            }
                        }
                    }
                }
            } else {// 若司机已经接了一个乘客，则只能再接一个乘客
                // 遍历第二个乘客
                if (inst.match_flag >= 1) {
                    for (int j2 = 0; j2 < nPassengers; j2++) {
                        double etaAim2 = inst.dpTimeMatrix[i][j2];
                        double sameAim = inst.dpValidMatrix[i][j2];
                        if (etaAim2 <= Param.MAX_ETA2 && sameAim > 0) {
                            // 生成一个司机带两个乘客的拼车方案
                            Pattern pattern2 = new Pattern(inst.driverList.get(i), null, inst.passengerList.get(j2));
                            pattern2.setAim(sameAim, Param.MAX_ETA, etaAim2);
                            pattern2.setIdx(i, -1, j2);
                            pattern2.setCur_time(inst.cur_time);
                            pool.add(pattern2);
                        }
                    }
                }
            }
        }
//        pool.sort(Comparator.comparing(o -> -o.aim));
        return pool;
    }

    public void branch() {

    }
}
