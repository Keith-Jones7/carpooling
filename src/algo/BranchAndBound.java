package algo;

import common.Param;
import ilog.concert.IloException;
import model.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;

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
        if (Param.MATCH_ALGO == 1) {
            bestSol = solve();
//            bestSol = solveNew();
        } else {
            bestSol = solveCplex();
        }

    }

    private Solution solve() {
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> carPool = new ArrayList<>();
        ArrayList<Pattern> totalPool = genAllPatterns(poolPQ, carPool);
        // CG求解LP
        ArrayList<Pattern> pool = genInitPatternsGreedy(poolPQ);
//        totalPool.removeAll(pool);
        LPSol lpSol = cg.solve(totalPool, totalPool);
        return divingHeur.solve(lpSol, Integer.MAX_VALUE);
    }

    private Solution solveNew() {
        // 生成所有pattern
        PriorityQueue<Pattern> carPoolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> totalCarPool = new ArrayList<>();
        ArrayList<Pattern> totalPool = genAllPatterns(carPoolPQ, totalCarPool);
        // 生成初始pattern
//        ArrayList<Pattern> pool = genInitCarPoolPatterns(carPoolPQ);
//        totalCarPool.removeAll(pool);
        // cg求解松弛解
        LPSol lpSol = cg.solve(totalCarPool, totalCarPool);
        // 潜水器启发式求解整数解
        Solution carPoolSol = divingHeur.solve(lpSol, Integer.MAX_VALUE);
        // km求解剩余整数解
        return genFinalSol(carPoolSol, totalPool);
    }

    Solution solveCplex() {
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> carPool = new ArrayList<>();
        ArrayList<Pattern> pool = genAllPatterns(poolPQ, carPool);
        try {
            rmpCplex.addColumns(pool);
            return rmpCplex.solveIP();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
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
        double time1 = Param.getTimeCost(s1);
        for (Pattern pattern : singlePool) {
            int driverIdx = pattern.driverIdx;
            int passenger1Idx = pattern.passenger1Idx;
            if (matchMatrix[driverIdx][passenger1Idx] == 1) {
                pool.add(pattern);
            }
        }
        double time = Param.getTimeCost(s);
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
        for (Pattern pattern : totalPool) {
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
        double timeCost = Param.getTimeCost(s);
        return new Solution(patterns, profit);
    }

    public ArrayList<Pattern> genAllPatterns(PriorityQueue<Pattern> carPoolPQ, ArrayList<Pattern> carPool) {
        long s = System.currentTimeMillis();
        ArrayList<Pattern> pool = new ArrayList<>();
        // 一个乘客上车的方案
        for (int j = 0; j < nPassengers; j++) {
            for (int i = 0; i < nDrivers; i++) {
                Driver driver = inst.driverList.get(i);
                Passenger passenger = inst.passengerList.get(j);
                // 一个乘客上空车
                if (driver.queue.size() == 0) {
                    double etaAim1 = inst.dpTimeMatrix[i][j];
                    if (etaAim1 <= Param.MAX_ETA) {
                        // 生成一个司机只带一个顾客的方案
                        Pattern pattern1 = new Pattern(driver, passenger, null);
                        pattern1.setAim(0.0, etaAim1, Param.MAX_ETA2);
                        pattern1.setIdx(i, j, -1);
                        pattern1.setCur_time(inst.cur_time);
                        pool.add(pattern1);
                    }
                }
                // 一个乘客上载人车
                else if (driver.queue.size() == 1 && inst.match_flag >= 1) {
                    double etaAim2 = inst.dpTimeMatrix[i][j];
                    double sameAim = inst.dpValidMatrix[i][j];
                    if (etaAim2 <= Param.MAX_ETA2 && sameAim > 0) {
                        // 生成一个司机带两个乘客的拼车方案
                        Pattern pattern2 = new Pattern(driver, null, passenger);
                        pattern2.setAim(sameAim, Param.MAX_ETA, etaAim2);
                        pattern2.setIdx(i, -1, j);
                        pattern2.setCur_time(inst.cur_time);
                        pool.add(pattern2);
                    }
                }
            }
        }
        // 两个乘客上车方案
        if (inst.match_flag >= 2) {
            for (int j1 = 0; j1 < nPassengers; j1++) {
                for (int j2 = j1 + 1; j2 < nPassengers; j2++) {
                    // 遍历第二个乘客，如果满足绕行约束和eta约束，则生成拼车pattern放入pool中
                    double sameAim12 = inst.ppValidMatrix[j1][j2];
                    double sameAim21 = inst.ppValidMatrix[j2][j1];
                    double etaAim12 = inst.ppTimeMatrix[j1][j2];
                    double etaAim21 = inst.ppTimeMatrix[j2][j1];
                    if (sameAim12 > 0 && (sameAim12 <= sameAim21 || sameAim21 == 0) && etaAim12 <= Param.MAX_ETA2) {
                        // 以12为准
                        for (int i = 0; i < nDrivers; i++) {
                            if (inst.driverList.get(i).queue.size() == 0) {
                                double etaAim1 = inst.dpTimeMatrix[i][j1];
                                if (etaAim1 <= Param.MAX_ETA) {
                                    // 生成一个司机带两个乘客的拼车方案
                                    Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j1), inst.passengerList.get(j2));
                                    pattern2.setAim(sameAim12, etaAim1, etaAim12);
                                    pattern2.setIdx(i, j1, j2);
                                    pattern2.setCur_time(inst.cur_time);
                                    pool.add(pattern2);
//                                    carPoolPQ.add(pattern2);
                                    carPool.add(pattern2);
                                }
                            }
                        }
                    } else if ((sameAim21 > 0) && (sameAim21 < sameAim12 || sameAim12 == 0) && etaAim21 <= Param.MAX_ETA2) {
                        // 以21为准
                        for (int i = 0; i < nDrivers; i++) {
                            if (inst.driverList.get(i).queue.size() == 0) {
                                double etaAim1 = inst.dpTimeMatrix[i][j2];
                                if (etaAim1 <= Param.MAX_ETA) {
                                    // 生成一个司机带两个乘客的拼车方案
                                    Pattern pattern2 = new Pattern(inst.driverList.get(i), inst.passengerList.get(j2), inst.passengerList.get(j1));
                                    pattern2.setAim(sameAim21, etaAim1, etaAim21);
                                    pattern2.setIdx(i, j2, j1);
                                    pattern2.setCur_time(inst.cur_time);
                                    pool.add(pattern2);
//                                    carPoolPQ.add(pattern2);
                                    carPool.add(pattern2);
                                }
                            }
                        }
                    }
                }
            }
        }
        return pool;
    }
}
