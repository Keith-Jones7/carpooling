package algo;

import common.Param;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import model.Instance;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;

public class RestrictMasterProblem {
    /**
     * 问题信息
     */
    Instance inst;     // 问题实例
    int nPassengers;    // 乘客数量
    int nDrivers;   // 司机数量

    /**
     * cplex求解相关信息
     */
    IloCplex cplex;     // cplex环境
    IloObjective obj;   // 目标
    IloRange[] ranges;   // 约束
    ArrayList<IloNumVar> x;   // 变量
    ArrayList<IloConversion> xConv;   // 转换变量
    IloNumVar[] artificialVars;   // 虚拟变量
    ArrayList<Pattern> pool;   // 所有方案集合

    /**
     * 固定参数
     */
    private final double IloInfinity = Double.MAX_VALUE;     // cplex中Double最大值
    private final double bigM = 1e8;   // 虚拟变量惩罚系数

    /**
     * 构造函数
     * @param inst
     */
    public RestrictMasterProblem(Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        try {
            formulate();
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * 建立集合划分模型
     */
    void formulate() throws IloException {
        cplex = new IloCplex();
        x = new ArrayList<>();
        xConv = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new IloRange[nDrivers + nPassengers];
        // objective
        obj = cplex.addMaximize();

        // constraints
        // (1): the driver constraint
        for (int i = 0; i < nDrivers; i++) {
            ranges[i] = cplex.addRange(0, 1, "rangeOnDriver" + i);
        }
        // (2): the passenger constraint
        for (int j = 0; j < nPassengers; j++) {
            ranges[nDrivers + j] = cplex.addRange(0, 1, "rangeOnPassenger" + j);
        }
        addArtificialVariables();

        cplex.setOut(null);
        cplex.setWarning(null);
        cplex.setParam(IloCplex.IntParam.RandomSeed, Param.SEED);
        cplex.setParam(IloCplex.IntParam.RootAlgorithm, IloCplex.Algorithm.Primal);
    }

    /**
     * 添加虚拟变量
     */
    void addArtificialVariables() throws IloException {
        artificialVars = new IloNumVar[nDrivers + nPassengers];
        // artificial var added in the constraints(1)
        for (int i = 0; i < nDrivers; i++) {
            IloColumn col = cplex.column(obj, -bigM);
            col = col.and(cplex.column(ranges[i], 1));
            artificialVars[i] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnDriver" + i);
        }
        // artificial var added in the constraints(2)
        for (int j = 0; j < nPassengers; j++) {
            IloColumn col = cplex.column(obj, -bigM);
            col = col.and(cplex.column(ranges[nDrivers + j], 1));
            artificialVars[nDrivers + j] = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, "avOnPassenger" + j);
        }
    }

    /**
     * 添加列变量
     * @param patterns 待添加的方案
     */
    void addColumns(ArrayList<Pattern> patterns) throws IloException {
        for (Pattern pattern : patterns) {
            pool.add(pattern);
            // add columns and vars
            IloColumn col = cplex.column(obj, pattern.aim);
            // range on driver
            col = col.and(cplex.column(ranges[pattern.driverIdx], 1));
            // range on two passengers
            if (pattern.passenger1Id >= 0) {
                col = col.and(cplex.column(ranges[nDrivers + pattern.passenger1Idx], 1));
            }
            if (pattern.passenger2Id >= 0) {
                col = col.and(cplex.column(ranges[nDrivers + pattern.passenger2Idx], 1));
            }
            // new column var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            IloNumVar var = cplex.numVar(col, 0, IloInfinity, IloNumVarType.Float, name);
            pattern.colVar = var;
            x.add(var);
        }
    }

    /**
     * 求解整数规划
     * @return 返回整数最优解
     */
    Solution solveIP() throws IloException {
        Solution sol = null;
        convertToIP();
        cplex.solve();
        boolean feasible = isModelFeasible();
        if (feasible) {
            sol = getIPSol();
        }
        convertToLP();
        return sol;
    }

    /**
     * 转变为线性规划
     */
    private void convertToIP() {
        try {
            for (IloNumVar iloNumVar : x) {
                IloConversion conv = cplex.conversion(iloNumVar, IloNumVarType.Int);
                cplex.add(conv);
                xConv.add(conv);
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 转变为整数规划
     */
    private void convertToLP() {
        try {
            for (IloConversion iloConversion : xConv) {
                cplex.remove(iloConversion);
            }
            xConv.clear();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断模型是否可行：当且仅当所有虚拟变量取值均为0的时候模型可行
     * @return 返回是否可行
     */
    boolean isModelFeasible() throws IloException {
        // check artificial on drivers
        for (int i = 0; i < nDrivers; i++) {
            double val = cplex.getValue(artificialVars[i]);
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        // check artificial on passengers
        for (int j = 0; j < nPassengers; j++) {
            double val = cplex.getValue(artificialVars[nDrivers + j]);
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取整数规划解
     * @return 返回整数最优解
     */
    Solution getIPSol() throws IloException {
        double objVal = cplex.getObjValue();
        Solution sol = new Solution();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = cplex.getValue(x.get(p));
            if (Param.equals(val, 1)) {
                sol.patterns.add(pattern);
            }
        }
        sol.profit = objVal;
        return sol;
    }

    /**
     * 求解结束
     */
    void end() {
        cplex.end();
    }
}
