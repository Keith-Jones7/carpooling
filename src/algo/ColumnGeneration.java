package algo;

import common.Param;
import model.Instance;
import model.Pattern;

import java.util.ArrayList;
import java.util.BitSet;

public class ColumnGeneration {
    /**
     * 问题的基本信息
     */
    Instance inst;          // 问题实例
    int nDrivers;   // 司机数量
    int nPassengers;  // 乘客数量


    /**
     * 求解模块
     */
    RMP_SCIP rmp;   // 主问题求解模块
    RestrictMasterProblem rmpCplex;   // cplex求解模块
    PricingProblem pp;  // 子问题求解模块

    /**
     * 构造函数
     * @param inst
     * @param rmp
     * @param rmpCplex
     * @param pp
     */
    public ColumnGeneration(Instance inst, RMP_SCIP rmp, RestrictMasterProblem rmpCplex, PricingProblem pp) {
        this.inst = inst;
        this.nDrivers = inst.driverList.size();
        this.nPassengers = inst.passengerList.size();
        this.rmp = rmp;
        this.rmpCplex = rmpCplex;
        this.pp = pp;
    }

    /**
     * 列生成求解函数
     * @param pool 初始解中的方案
     * @param totalPool 所有方案
     * @return 返回松弛解
     */
    LPSol solve(ArrayList<Pattern> pool, ArrayList<Pattern> totalPool) {
        rmp.addColumns(pool);
        pp.set(totalPool);
        return cg();
    }

    /**
     * 列生成主算法
     * @return 返回松弛解
     */
    LPSol cg() {
        double[] dualsOfRanges;
        BitSet fixedDrivers;
        BitSet fixedPassengers;
        double obj;
        long s0 = System.currentTimeMillis();
        rmp.solveLP();
        obj = rmp.getObjVal();
//        System.out.println(rmp.pool.size() + " " + obj);
        dualsOfRanges = rmp.getDualsOfRanges();
        fixedDrivers = rmp.fixedDrivers;
        fixedPassengers = rmp.fixedPassengers;

        pp.solve(dualsOfRanges, fixedDrivers, fixedPassengers);
        while (pp.findNewColumns() && Param.getTimeCost(s0) < 10) {
            rmp.addColumns(pp.patterns);
            rmp.solveLP();
            obj = rmp.getObjVal();
//            System.out.println(rmp.pool.size() + " " + obj);
            dualsOfRanges = rmp.getDualsOfRanges();
            pp.solve(dualsOfRanges, fixedDrivers, fixedPassengers);
        }
        return rmp.getLPSol();
    }

    /**
     * 求解结束释放求解器
     */
    void end() {
        rmp.end();
    }
}
