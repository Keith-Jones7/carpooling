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

//    public ArrayList<Pattern> genInitPatterns() {
//        ArrayList<Pattern> pool = new ArrayList<>();
//
//        // greedy
//        BitSet passengerBit = new BitSet(nPassengers);
//        BitSet driverBit = new BitSet(nDrivers);
//        boolean remainPattern = true;
//        while (remainPattern) {
//            remainPattern = false;
//            double maxAim = -Double.MAX_VALUE;
//            int driverId = -1;
//            int passenger1Id = -1;
//            int passenger2Id = -1;
//            double sameTime = 0;
//            double getTime = 0;
//            // 遍历所有顾客对
//            for (int i = 0; i < nPassengers; i++) {
//                for (int j = 0; j < nPassengers; j++) {
//                    // 当两个顾客尚未被服务，且满足拼车条件时
//                    if (i != j && inst.ppValidMatrix[i][j] > 0 && !passengerBit.get(i) && !passengerBit.get(j)) {
//                        for (int k = 0; k < nDrivers; k++) {
//                            if (!driverBit.get(k)) {
//                                double aim = Param.obj1Coef * inst.ppValidMatrix[i][j] - inst.dpTimeMatrix[k][i];
//                                if (aim > maxAim) {
//                                    maxAim = aim;
//                                    driverId = k;
//                                    passenger1Id = i;
//                                    passenger2Id = j;
//                                    remainPattern = true;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            if (remainPattern) {
//                driverBit.set(driverId);
//                passengerBit.set(passenger1Id);
//                passengerBit.set(passenger2Id);
//
//                Pattern pattern = new Pattern(driverId, passenger1Id, passenger2Id);
//                pattern.setTime(sameTime, getTime);
//                pool.add(pattern);
//            }
//        }
//        return pool;
//    }

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
                        pattern1.setAim(0.0, etaAim1);
                        pattern1.setIdx(i, j1, -1);
                        pool.add(pattern1);
                        // 遍历第二个乘客，如果满足绕行约束和eta约束，则生成拼车pattern放入pool中
                        for (int j2 = 0; j2 < nPassengers && j2 != j1; j2++) {
                            double etaAim = inst.ppTimeMatrix[j1][j2];
                            double sameAim = inst.ppValidMatrix[j1][j2];
                            if (etaAim <= Param.MAX_ETA2 && sameAim > 0) {
                                // 生成一个司机带两个乘客的拼车方案
                                Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), inst.passengerList.get(j2));
                                pattern2.setAim(sameAim, etaAim);
                                pattern2.setIdx(i, j1, j2);
                                pool.add(pattern2);
                            }
                        }
                    }
                }
            } else {// 若司机已经接了一个乘客，则只能再接一个乘客
                // 遍历第二个乘客
                for (int j2 = 0; j2 < nPassengers; j2++) {
                    double etaAim2 = inst.dpTimeMatrix[i][j2];
                    double sameAim = inst.dpValidMatrix[i][j2];
                    if (etaAim2 <= Param.MAX_ETA2 && sameAim > 0) {
                        // 生成一个司机带两个乘客的拼车方案
                        Pattern pattern2 = new Pattern(inst.driverList.get(i), null, inst.passengerList.get(j2));
                        pattern2.setAim(sameAim, etaAim2);
                        pattern2.setIdx(i, -1, j2);
                        pool.add(pattern2);
                    }
                }
            }
        }
        return pool;
    }

    public void branch() {

    }
}
