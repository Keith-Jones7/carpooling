package algo;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import common.Param;
import javafx.util.Pair;
import model.Instance;
import model.Pattern;
import model.Solution;

import java.util.ArrayList;
import java.util.BitSet;

public class RMP_SCIP {
    /**
     * 问题信息
     */
    Instance inst;     // 问题实例
    int nPassengers;    // 乘客数量
    int nDrivers;   // 司机数量

    /**
     * ortools求解相关信息
     */
    MPSolver solver;   // ortools环境
    MPObjective obj;    // 目标
    MPConstraint[] ranges;    // 约束
    ArrayList<MPVariable> x;    // 变量
    MPVariable[] artificialVars;    // 虚拟变量
    ArrayList<Pattern> pool;    // 所有方案集合

    /**
     * 潜水启发式相关信息
     */
    BitSet fixedDrivers;          // 潜水固定的司机索引
    BitSet fixedPassengers;      // 潜水固定的乘客索引

    /**
     * 固定参数
     */
    private final double infinity = 1e8;     // ortools中Double最大值
    private final double bigM = 1e8;     // 虚拟变量惩罚系数

    /**
     * 构造函数
     * @param inst
     */
    public RMP_SCIP(Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        this.fixedDrivers = new BitSet(nDrivers);
        this.fixedPassengers = new BitSet(nPassengers);
        formulate();
    }

    /**
     * 建立集合划分模型
     */
    void formulate() {
        // init
        Loader.loadNativeLibraries();
        String solverName = "GLOP";
        solver = MPSolver.createSolver(solverName);

        x = new ArrayList<>();
        pool = new ArrayList<>();
        ranges = new MPConstraint[nDrivers + nPassengers];
        // objective
        obj = solver.objective();
        obj.setMaximization();
        // constraints
        // (1): the driver constraint
        for (int i = 0; i < nDrivers; i++) {
            ranges[i] = solver.makeConstraint(0, 1, "rangeOnDriver" + i);
        }
        // (2): the passenger constraint
        for (int j = 0; j < nPassengers; j++) {
            ranges[nDrivers + j] = solver.makeConstraint(0, 1, "rangeOnPassenger" + j);
        }

        addArtificialVariable();
    }

    /**
     * 添加虚拟变量
     */
    void addArtificialVariable() {
        artificialVars = new MPVariable[nDrivers + nPassengers];
        // artificial var added in the constraints(1)
        for (int i = 0; i < nDrivers; i++) {
            MPVariable var = solver.makeVar(0, infinity, false, "avOnDriver" + i);
            obj.setCoefficient(var, -bigM);
            ranges[i].setCoefficient(var, 1);
            artificialVars[i] = var;
        }
        // artificial var added in the constraints(2)
        for (int j = 0; j < nPassengers; j++) {
            MPVariable var = solver.makeVar(0, infinity, false, "avOnPassenger" + j);
            obj.setCoefficient(var, -bigM);
            ranges[nDrivers + j].setCoefficient(var, 1);
            artificialVars[nDrivers + j] = var;
        }
    }

    /**
     * 应用潜水启发式得到的潜水变量
     * @param fixedIdx 潜水变量索引
     * @param vals 当前松弛解
     */
    public void setDiving(BitSet fixedIdx, ArrayList<Pair<Pattern, Double>> vals) {
        for (int h = fixedIdx.nextSetBit(0); h >= 0; h = fixedIdx.nextSetBit(h + 1)) {
            Pair<Pattern, Double> val = vals.get(h);
            MPVariable var = val.getKey().var;
            var.setBounds(1.0, 1.0 + Param.EPS);
            // fix drivers and passengers
            fixedDrivers.set(val.getKey().driverIdx);
            if (val.getKey().passenger1Idx >= 0) {
                fixedPassengers.set(val.getKey().passenger1Idx);
            }
            if (val.getKey().passenger2Idx >= 0) {
                fixedPassengers.set(val.getKey().passenger2Idx);
            }
        }
    }

    /**
     * 添加列变量
     * @param patterns 待添加的方案
     */
    void addColumns(ArrayList<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            // 测试：不能在pool中加入重复pattern
            if (pool.contains(pattern)) {
                System.out.println("error: pattern " + pattern.toString() + "already in pool");
            }
            pool.add(pattern);
            // add var
            String name = "x_" + pattern.driverIdx + "," + pattern.passenger1Idx + "," + pattern.passenger2Idx;
            MPVariable var = solver.makeVar(0, infinity, false, name);
            // set obj
            obj.setCoefficient(var, pattern.aim);
            // set range on driver
            ranges[pattern.driverIdx].setCoefficient(var, 1);
            // set range on two passengers
            if (pattern.passenger1Idx >= 0) {
                ranges[nDrivers + pattern.passenger1Idx].setCoefficient(var, 1);
            }
            if (pattern.passenger2Idx >= 0) {
                ranges[nDrivers + pattern.passenger2Idx].setCoefficient(var, 1);
            }
            pattern.var = var;
            x.add(var);
        }
    }

    /**
     * 求解线性规划模型
     * @return 返回线性松弛解
     */
    LPSol solveLP() {
//        MPSolverParameters parameters = new MPSolverParameters();
//        parameters.setIntegerParam(MPSolverParameters.IntegerParam.LP_ALGORITHM, 10);
        solver.solve();
        return getLPSol();
    }

    /**
     * 求解整数规划
     * @return 返回整数最优解
     */
    Solution solveIP() {
//        MPSolverParameters parameters = new MPSolverParameters();
//        parameters.setIntegerParam(MPSolverParameters.IntegerParam.LP_ALGORITHM, 11);
        // solve
        convertToIP();
        solver.solve();
        return getIPSol();
    }

    /**
     * 转变为整数规划
     */
    private void convertToIP() {
        for (MPVariable var : x) {
            var.setInteger(true);
        }
    }

    /**
     * 转变为线性规划
     */
    private void convertToLP() {
        for (MPVariable var : x) {
            var.setInteger(false);
        }
    }

    /**
     * 判断模型是否可行：当且仅当所有虚拟变量取值均为0的时候模型可行
     * @return 返回是否可行
     */
    boolean isModelFeasible() {
        // check artificial on drivers
        for (int i = 0; i < nDrivers; i++) {
            double val = artificialVars[i].solutionValue();
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        // check artificial on passengers
        for (int j = 0; j < nPassengers; j++) {
            double val = artificialVars[nDrivers + j].solutionValue();
            if (!Param.equals(val * bigM, 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取对偶变量
     * @return 返回对偶变量
     */
    double[] getDualsOfRanges() {
        double[] dualsOfRanges = new double[nDrivers + nPassengers];
        for (int i = 0; i < nDrivers + nPassengers; i++) {
            dualsOfRanges[i] = ranges[i].dualValue();
        }
        return dualsOfRanges;
    }

    /**
     * 获取最优目标值
     * @return 返回目标值
     */
    double getObjVal() {
        return obj.value();
    }

    /**
     * 获取线性规划解
     * @return 返回线性松弛解
     */
    LPSol getLPSol() {
        double objVal = obj.value();
        ArrayList<Pair<Pattern, Double>> vals = new ArrayList<>();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = x.get(p).solutionValue();
            if (val > Param.EPS) {
                vals.add(new Pair<>(pattern, val));
            }
        }
        return new LPSol(vals, objVal);
    }

    /**
     * 获取整数规划解
     * @return 返回整数最优解
     */
    Solution getIPSol() {
        double objVal = obj.value();
        Solution sol = new Solution();
        for (int p = 0; p < pool.size(); p++) {
            Pattern pattern = pool.get(p);
            double val = x.get(p).solutionValue();
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
        solver.clear();
    }
}
