package algo;

import common.Param;
import ilog.concert.IloException;
import model.*;

import javax.xml.soap.SAAJMetaFactory;
import java.util.*;

public class BranchAndBound {

    public Instance inst;
    public int nDrivers;
    public int nPassengers;
    public Solution bestSol;

    public RMP_SCIP rmp;
    public RestrictMasterProblem rmpCplex;
    public PricingProblem pp;
    public DivingHeuristic divingHeur;
    public ColumnGeneration cg;
    public BranchAndBound(Instance inst) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = new RMP_SCIP(inst);
        this.rmpCplex = new RestrictMasterProblem(inst);
        this.pp = new PricingProblem(inst);
        this.divingHeur = new DivingHeuristic(inst, rmp, rmpCplex, pp);
        this.cg = new ColumnGeneration(inst, rmp, rmpCplex, pp);
        this.bestSol = new Solution();
    }

    public void run() {
        if (Param.USE_CG) {
//            bestSol = solve();
            bestSol = solveNew();
        } else {
            bestSol = genMIPSol();
        }

    }

    private Solution solve() {
//        ArrayList<Pattern> pool = genInitPatterns();
        //ArrayList<Pattern> pool = genAllPatterns();
        long s0 = System.currentTimeMillis();
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> carPool = new ArrayList<>();
        ArrayList<Pattern> totalPool = genAllPatterns(poolPQ, carPool);
        double timeCost0 = Param.getTimecost(s0);
        // CG求解LP
        long s1 = System.currentTimeMillis();
        ArrayList<Pattern> pool = genInitPatternsGreedyKM(poolPQ);
        double timeCost1 = Param.getTimecost(s1);
//            ArrayList<Pattern> pool = new ArrayList<>();
//        totalPool.removeAll(pool);
//            totalPool.clear();
        long s2 = System.currentTimeMillis();
        LPSol lpSol = cg.solve(totalPool, totalPool);
//            lpSol.vals.sort(Comparator.comparing(o -> o.getKey().driverId));
        double timeCost2 = Param.getTimecost(s2);
        return divingHeur.solve(lpSol, Integer.MAX_VALUE);
    }

    private Solution solveNew() {
//        ArrayList<Pattern> pool = genInitPatterns();
        //ArrayList<Pattern> pool = genAllPatterns();
        // 生成所有pattern
        long s0 = System.currentTimeMillis();
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> totalCarPool = new ArrayList<>();
        ArrayList<Pattern> totalPool = genAllPatterns(poolPQ, totalCarPool);
        double timeCost0 = Param.getTimecost(s0);
        // 生成初始pattern
        long s1 = System.currentTimeMillis();
        ArrayList<Pattern> pool = genInitCarPoolPatterns(poolPQ);
        double timeCost1 = Param.getTimecost(s1);
//        totalCarPool.removeAll(pool);
        // cg求解松弛解
        long s2 = System.currentTimeMillis();
        LPSol lpSol = cg.solve(totalCarPool, totalCarPool);
        double timeCost2 = Param.getTimecost(s2);
        // 潜水器启发式求解整数解
        long s3 = System.currentTimeMillis();
        Solution carPoolSol = divingHeur.solve(lpSol, Integer.MAX_VALUE);
        double timeCost3 = Param.getTimecost(s3);
        // km求解剩余整数解
        long s4 = System.currentTimeMillis();
        Solution sol = genFinalSol(carPoolSol, totalPool);
        double timeCost4 = Param.getTimecost(s4);
        return sol;
    }

    public void updateUB() {

    }

    public void updateLB() {

    }

    public void createRoot() {

    }

    Solution genMIPSol() {
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> carPool = new ArrayList<>();
        ArrayList<Pattern> pool = genAllPatterns();
        if (Param.LP_IP) {
            long s0 = System.currentTimeMillis();
            rmp.addColumns(pool);
            LPSol lpSol = rmp.solveLP();
            double timeCost = Param.getTimecost(s0);
            return divingHeur.solve(lpSol, Integer.MAX_VALUE);
        } else {
            try {
                rmpCplex.addColumns(pool);
                return rmpCplex.solveIP();
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
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
        long s0 = System.currentTimeMillis();
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
                            for (int j2 = j1 + 1; j2 < nPassengers; j2++) {
                                double etaAim2 = Double.MAX_VALUE;
                                double sameAim = 0;
                                double sameAim12 = inst.ppValidMatrix[j1][j2];
                                double sameAim21 = inst.ppValidMatrix[j2][j1];

                                if (sameAim12 > 0 && (sameAim12 < sameAim21 || sameAim21 == 0)) {
                                    // 以12为准
                                    sameAim = sameAim12;
                                    etaAim1 = inst.dpTimeMatrix[i][j1];
                                    etaAim2 = inst.ppTimeMatrix[j1][j2];
                                    if (etaAim1 <= Param.MAX_ETA && etaAim2 <= Param.MAX_ETA2) {
                                        // 生成一个司机带两个乘客的拼车方案
                                        Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), inst.passengerList.get(j2));
                                        pattern2.setAim(sameAim, etaAim1, etaAim2);
                                        pattern2.setIdx(i, j1, j2);
                                        pattern2.setCur_time(inst.cur_time);
                                        pool.add(pattern2);
                                    }

                                }
                                else if ((sameAim21 > 0) && (sameAim21 <= sameAim12 || sameAim12 == 0)) {
                                    // 以21为准
                                    sameAim = sameAim21;
                                    etaAim1 = inst.dpTimeMatrix[i][j2];
                                    etaAim2 = inst.ppTimeMatrix[j2][j1];
                                    if (etaAim1 <= Param.MAX_ETA && etaAim2 <= Param.MAX_ETA2) {
                                        // 生成一个司机带两个乘客的拼车方案
                                        Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j2), inst.passengerList.get(j1));
                                        pattern2.setAim(sameAim, etaAim1, etaAim2);
                                        pattern2.setIdx(i, j2, j1);
                                        pattern2.setCur_time(inst.cur_time);
                                        pool.add(pattern2);
                                    }
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
//        pool.sort(Comparator.comparing(o -> o.driverId));
        Param.timeCostOnGenPatterns += Param.getTimecost(s0);
//        System.out.println(pool.size());
        return pool;
    }

    public ArrayList<Pattern> genInitPatternsGreedy(PriorityQueue<Pattern> poolPQ) {
        ArrayList<Pattern> pool = new ArrayList<>();

        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        while (poolPQ.size() > 0) {
            Pattern pattern = poolPQ.peek();
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            int passenger2Idx = pattern.passenger2Idx;
            boolean driverAvailable = !driverBit.get(driverIdx);
            boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
            boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
            if (driverAvailable && passenger1Available && passenger2Available) {
                pool.add(pattern);
                driverBit.set(pattern.driverIdx);
                if (pattern.passenger1Idx >= 0) {
                    passengerBit.set(pattern.passenger1Idx);
                }
                if (pattern.passenger2Idx >= 0) {
                    passengerBit.set(pattern.passenger2Idx);
                }
            }
            poolPQ.poll();
        }
        return pool;
    }

    public ArrayList<Pattern> genInitPatternsGreedyKM(PriorityQueue<Pattern> poolPQ) {
        ArrayList<Pattern> pool = new ArrayList<>();

        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        // 每次将拼车方案拿出来，直到遇到非拼车方案
        while (poolPQ.size() > 0) {
            Pattern pattern = poolPQ.peek();
            if (pattern.passenger2Id == -1) {
                break;
            }
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            int passenger2Idx = pattern.passenger2Idx;
            boolean driverAvailable = !driverBit.get(driverIdx);
            boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
            boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
            if (driverAvailable && passenger1Available && passenger2Available) {
                pool.add(pattern);
                driverBit.set(pattern.driverIdx);
                if (pattern.passenger1Idx >= 0) {
                    passengerBit.set(pattern.passenger1Idx);
                }
                if (pattern.passenger2Idx >= 0) {
                    passengerBit.set(pattern.passenger2Idx);
                }
            }
            poolPQ.poll();
        }
        long s = System.currentTimeMillis();
        // 将剩下的非拼车方案用KM求解
        // 构建weight矩阵
        double[][] weight = new double[nDrivers][nPassengers];
        ArrayList<Pattern> singlePool = new ArrayList<>();
        // Todo: 可优化
        while (poolPQ.size() > 0) {
            Pattern pattern = poolPQ.peek();
            assert pattern.passenger2Id == -1;
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            boolean driverAvailable = !driverBit.get(driverIdx);
            boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
            if (driverAvailable && passenger1Available) {
                weight[driverIdx][passenger1Idx] = pattern.aim;
                singlePool.add(pattern);
            }
            poolPQ.poll();
        }
        long s1 = System.currentTimeMillis();
        KMAlgorithm kmAlgo = new KMAlgorithm(weight);
        int[][] matchMatrix = kmAlgo.getMatch();
        double time1 = Param.getTimecost(s1);
        for (Pattern pattern : singlePool) {
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            if (matchMatrix[driverIdx][passenger1Idx] == 1) {
                pool.add(pattern);
            }
        }
        double time = Param.getTimecost(s);
        return pool;
    }


    public ArrayList<Pattern> genAllPatterns(PriorityQueue<Pattern> poolPQ, ArrayList<Pattern> carPool) {
        long s = System.currentTimeMillis();
        double timeCost0 = 0;
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
                        poolPQ.add(pattern1);

                        // 遍历第二个乘客，如果满足绕行约束和eta约束，则生成拼车pattern放入pool中
                        if (inst.match_flag >= 2) {
                            for (int j2 = j1 + 1; j2 < nPassengers; j2++) {
                                double etaAim2 = Double.MAX_VALUE;
                                double sameAim = 0;
                                double sameAim12 = inst.ppValidMatrix[j1][j2];
                                double sameAim21 = inst.ppValidMatrix[j2][j1];

                                if (sameAim12 > 0 && (sameAim12 < sameAim21 || sameAim21 == 0)) {
                                    // 以12为准
                                    sameAim = sameAim12;
                                    etaAim1 = inst.dpTimeMatrix[i][j1];
                                    etaAim2 = inst.ppTimeMatrix[j1][j2];
                                    if (etaAim1 <= Param.MAX_ETA && etaAim2 <= Param.MAX_ETA2) {
                                        // 生成一个司机带两个乘客的拼车方案
                                        Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), inst.passengerList.get(j2));
                                        pattern2.setAim(sameAim, etaAim1, etaAim2);
                                        pattern2.setIdx(i, j1, j2);
                                        pattern2.setCur_time(inst.cur_time);
                                        pool.add(pattern2);
                                    }

                                }
                                else if ((sameAim21 > 0) && (sameAim21 <= sameAim12 || sameAim12 == 0)) {
                                    // 以21为准
                                    sameAim = sameAim21;
                                    etaAim1 = inst.dpTimeMatrix[i][j2];
                                    etaAim2 = inst.ppTimeMatrix[j2][j1];
                                    if (etaAim1 <= Param.MAX_ETA && etaAim2 <= Param.MAX_ETA2) {
                                        // 生成一个司机带两个乘客的拼车方案
                                        Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j2), inst.passengerList.get(j1));
                                        pattern2.setAim(sameAim, etaAim1, etaAim2);
                                        pattern2.setIdx(i, j2, j1);
                                        pattern2.setCur_time(inst.cur_time);
                                        pool.add(pattern2);
                                    }
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
                            poolPQ.add(pattern2);
                        }
                    }
                }
            }
        }
        double timeCost = Param.getTimecost(s);
        return pool;
    }

    public ArrayList<Pattern> genInitCarPoolPatterns(PriorityQueue<Pattern> poolPQ) {
        ArrayList<Pattern> carPool = new ArrayList<>();

        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        // 每次将拼车方案拿出来，直到遇到非拼车方案
        while (poolPQ.size() > 0) {
            Pattern pattern = poolPQ.peek();
            if (pattern.passenger2Id == -1) {
                break;
            }
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            int passenger2Idx = pattern.passenger2Idx;
            boolean driverAvailable = !driverBit.get(driverIdx);
            boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
            boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
            if (driverAvailable && passenger1Available && passenger2Available) {
                carPool.add(pattern);
                driverBit.set(pattern.driverIdx);
                if (pattern.passenger1Idx >= 0) {
                    passengerBit.set(pattern.passenger1Idx);
                }
                if (pattern.passenger2Idx >= 0) {
                    passengerBit.set(pattern.passenger2Idx);
                }
            }
            poolPQ.poll();
        }
        return carPool;
    }

    public Solution genFinalSol(Solution carPoolSol, ArrayList<Pattern> totalPool) {
        long s = System.currentTimeMillis();
        ArrayList<Pattern> patterns = new ArrayList<>();
        double profit = 0;
        // greedy
        BitSet passengerBit = new BitSet(nPassengers);
        BitSet driverBit = new BitSet(nDrivers);
        // 先将所有拼车方案导入最终解，并更新bit
        for (Pattern pattern : carPoolSol.patterns) {
            patterns.add(pattern);
            profit += pattern.aim;
            driverBit.set(pattern.driverIdx);
            if (pattern.passenger1Idx >= 0) {
                passengerBit.set(pattern.passenger1Idx);
            }
            if (pattern.passenger2Idx >= 0) {
                passengerBit.set(pattern.passenger2Idx);
            }
        }
        //
        // 将剩下的非拼车方案用KM求解
        // 构建weight矩阵
        double[][] weight = new double[nDrivers][nPassengers];
        ArrayList<Pattern> singlePool = new ArrayList<>();
        // Todo: 可优化
        for (Pattern pattern : totalPool) {
            assert pattern.passenger2Id == -1;
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            int passenger2Idx = pattern.passenger2Idx;
            boolean driverAvailable = !driverBit.get(driverIdx);
            boolean passenger1Available = passenger1Idx == -1 || !passengerBit.get(passenger1Idx);
            boolean passenger2Available = passenger2Idx == -1 || !passengerBit.get(passenger2Idx);
            if (driverAvailable && passenger1Available && passenger2Available) {
                if (passenger1Idx >= 0) {
                    weight[driverIdx][passenger1Idx] = pattern.aim;
                } else {
                    weight[driverIdx][passenger2Idx] = pattern.aim;
                }

                singlePool.add(pattern);
            }
        }
        

        KMAlgorithm kmAlgo = new KMAlgorithm(weight);
        int[][] matchMatrix = kmAlgo.getMatch();

        for (Pattern pattern : singlePool) {
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            int passenger2Idx = pattern.passenger2Idx;
            if (passenger1Idx >= 0) {
                if (matchMatrix[driverIdx][passenger1Idx] == 1) {
                    patterns.add(pattern);
                    profit += pattern.aim;
                }
            } else {
                if (matchMatrix[driverIdx][passenger2Idx] == 1) {
                    patterns.add(pattern);
                    profit += pattern.aim;
                }
            }

        }
        double timeCost = Param.getTimecost(s);
        return new Solution(patterns, profit);
    }

    public void branch() {

    }
}
