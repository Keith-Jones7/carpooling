package algo;

import common.Param;
import ilog.concert.IloException;
import model.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;

public class BranchAndBound {

    /**
     * 问题的基本信息
     */
    public Instance inst;                                    // 问题实例
    public int nDrivers;                                     // 司机数量
    public int nPassengers; // 乘客数量
    public Solution bestSol;  // 保存最好的解

    /**
     * 求解模块
     */
    public RMP_SCIP rmp;    // 主问题求解模块
    public RestrictMasterProblem rmpCplex;    // cplex求解模块
    public PricingProblem pp;   // 子问题求解模块
    public DivingHeuristic divingHeur;  // 潜水启发式求解模块
    public ColumnGeneration cg;   // 列生成求解模块

    /**
     * 列生成+潜水启发式算法的构造函数
     * @param inst
     */
    public BranchAndBound(Instance inst) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.rmp = new RMP_SCIP(inst);
        this.rmpCplex = new RestrictMasterProblem(inst);
        this.pp = new PricingProblem(inst);
        this.divingHeur = new DivingHeuristic(inst, rmp, pp);
        this.cg = new ColumnGeneration(inst, rmp, rmpCplex, pp);
        this.bestSol = new Solution();
    }

    /**
     * 运行函数
     * MATCH_ALGO == 0 调用cplex求解函数solveCplex()
     * MATCH_ALGO == 1 调用列生成+潜水启发式求解函数solve()
     */
    public void run() {
        if (Param.MATCH_ALGO == 1) {
            bestSol = solve();
            cg.end();

        } else {
            bestSol = solveCplex();
            rmpCplex.end();
        }

    }

    /**
     * 列生成+潜水启发式求解函数
     * @return 返回求解结果
     */
    private Solution solve() {
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> totalPool = genAllPatterns(poolPQ);
        // CG求解LP
        ArrayList<Pattern> pool = genInitPatternsGreedy(poolPQ);
        totalPool.removeAll(pool);
        LPSol lpSol = cg.solve(pool, totalPool);
        return divingHeur.solve(lpSol, Integer.MAX_VALUE);
    }

    /**
     * cplex求解函数
     * @return 返回最优结果
     */
    Solution solveCplex() {
        PriorityQueue<Pattern> poolPQ = new PriorityQueue<>(Comparator.comparing(o -> -o.aim));
        ArrayList<Pattern> pool = genAllPatterns(poolPQ);
        try {
            rmpCplex.addColumns(pool);
            return rmpCplex.solveIP();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 贪心算法构造初始解：每次从优先级队列中选择最好的方案加入初始解
     * @param poolPQ 所有方案按照aim排序的优先级队列
     * @return 返回生成的初始解
     */
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

    /**
     * 生成所有可行的方案，包括拼车和不拼车
     * @param poolPQ 保存所有方案按照aim排序的优先级队列
     * @return 返回所有的方案
     */
    public ArrayList<Pattern> genAllPatterns(PriorityQueue<Pattern> poolPQ) {
        long s = System.currentTimeMillis();
        ArrayList<Pattern> pool = new ArrayList<>();
        // 考虑一个乘客上车的方案，上空车或者载人车
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
                        poolPQ.add(pattern1);
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
                        poolPQ.add(pattern2);
                    }
                }
            }
        }
        // 考虑两个乘客上车方案
        if (inst.match_flag >= 2) {
            for (int j1 = 0; j1 < nPassengers; j1++) {
                for (int j2 = j1 + 1; j2 < nPassengers; j2++) {
                    // 遍历第二个乘客，如果满足绕行约束和eta约束，则生成拼车pattern放入pool中
                    double sameAim12 = inst.ppValidMatrix[j1][j2];
                    double sameAim21 = inst.ppValidMatrix[j2][j1];
                    double etaAim12 = inst.ppTimeMatrix[j1][j2];
                    double etaAim21 = inst.ppTimeMatrix[j2][j1];
                    if (sameAim12 > 0 && (sameAim12 <= sameAim21 || sameAim21 == 0) && etaAim12 <= Param.MAX_ETA2) {
                        // 乘客按照12顺序上车的方案
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
                                    poolPQ.add(pattern2);
                                }
                            }
                        }
                    } else if ((sameAim21 > 0) && (sameAim21 < sameAim12 || sameAim12 == 0) && etaAim21 <= Param.MAX_ETA2) {
                        // 乘客按照21顺序上车的方案
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
                                    poolPQ.add(pattern2);
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
