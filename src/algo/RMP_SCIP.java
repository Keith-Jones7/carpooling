package algo;

import jscip.*;
import model.*;
import model.*;

import java.util.*;

public class RMP_SCIP {
    // instance
    Instance inst;
    int nPassengers;
    int nDrivers;

    // scip
    Scip scip;
    Constraint[] constraints;
    ArrayList<Variable> x;

    ArrayList<Pattern> pool;

    // diving heuristic
    BitSet fixedItems;



    // final param
    private final int ScipIntMax = Integer.MAX_VALUE;
    private final double ScipInfinity = Double.MAX_VALUE;
    private final double bigM = 1e8;

    public RMP_SCIP (Instance inst) {
        this.inst = inst;
        this.nPassengers = inst.nPassengers;
        this.nDrivers = inst.nDrivers;
        formulate();
    }

    void formulate() {

    }

    // 添加虚拟变量
    void addArtificialVariables() {

    }

    // 设置属性
    void set() {

    }

    // 设置潜水启发式
    void setDiving() {

    }

    // 移除潜水启发式
    void recoverDiving() {

    }

    // 添加列变量
    void addColumns(ArrayList<Pattern> patterns) {

    }

    void removeInvalidRanges(ArrayList<Pattern> allPatterns) {

    }

    // 求解线性规划
    void solveLP() {

    }

    // 求解整数规划
    Sol solveIP() {
        return null;
    }

    // 转变为线性规划
    private void convertToIP() {

    }

    // 转变为整数规划
    private void convertToLP() {

    }

    // 获取对偶变量
    double[] getDualsOfRanges() {
        return null;
    }

    // model feasible only when all artificial vars are 0
    boolean isModelFeasible() {
        // check artificial on drivers

        // check artificial on passengers

        return true;
    }

    // 获取最优目标值
    double getObjVal() {
        return 0;
    }

    // 获取线性规划解
    LPSol getLPSol() {
        return null;
    }

    // 获取整数规划解
    Sol getIPSol() {
        return null;
    }

    // 求解结束
    void end() {

    }
}
