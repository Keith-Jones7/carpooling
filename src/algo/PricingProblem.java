package algo;

import common.Param;
import model.Driver;
import model.Instance;
import model.Passenger;
import model.Pattern;

import java.util.*;

public class PricingProblem {
    /**
     * 问题信息
     */
    Instance inst;                    // 问题实例
    int nDrivers;  // 司机数量
    int nPassengers;  // 乘客数量
    List<Driver> driverList;  // 司机集合
    List<Passenger> passengerList;   // 乘客集合

    /**
     * 子问题求解相关信息
     */
    ArrayList<Pattern> totalPatterns;   // 所有方案集合
    double[] dualsOfRanges;  // 约束的对偶变量
    BitSet fixedDrivers;  // 潜水固定的司机索引
    BitSet fixedPassengers;  // 潜水固定的乘客索引
    ArrayList<Pattern> patterns;  // 加入主问题的方案
    int maxColNum = 1000;     // 每次迭代子问题生成列最大数量

    /**
     * 子问题构造函数
     * @param inst
     */
    public PricingProblem(Instance inst) {
        this.inst = inst;
        this.nDrivers = inst.nDrivers;
        this.nPassengers = inst.nPassengers;
        this.driverList = inst.driverList;
        this.passengerList = inst.passengerList;

        this.patterns = new ArrayList<>();
    }

    void set(ArrayList<Pattern> totalPatterns) {
        this.totalPatterns = totalPatterns;
    }

    /**
     * 子问题求解函数
     * @param dualsOfRanges 约束的对偶变量
     * @param fixedDrivers 潜水固定的司机
     * @param fixedPassengers 潜水固定的乘客
     */
    void solve(double[] dualsOfRanges, BitSet fixedDrivers, BitSet fixedPassengers) {
        // solve init
        this.dualsOfRanges = dualsOfRanges;
        this.fixedDrivers = fixedDrivers;
        this.fixedPassengers = fixedPassengers;
        patterns.clear();
        // solve
        PriorityQueue<Pattern> patternPriorityQueue = new PriorityQueue<>(Comparator.comparing(o -> o.reducedCost));
        for (Pattern pattern : totalPatterns) {
            if (containFixedItem(pattern)) {
                continue;
            }
            double reducedCost = computeReducedCost(pattern);
            pattern.reducedCost = reducedCost;
            // 判断检验数大于0
            if (reducedCost > Param.EPS) {
                // 判断是否加入优先级队列
                if (patternPriorityQueue.size() < maxColNum) {
                    patternPriorityQueue.add(pattern);
                } else {
                    if (reducedCost > patternPriorityQueue.peek().reducedCost) {
                        patternPriorityQueue.poll();
                        patternPriorityQueue.add(pattern);
                    }
                }
            }
        }
        patterns.addAll(patternPriorityQueue);
    }

    /**
     * 判断pattern中是否有已经被潜水固定的司机或乘客
     * @param pattern 当前方案
     * @return 返回boolean值
     */
    boolean containFixedItem(Pattern pattern) {
        if (fixedDrivers.get(pattern.driverIdx)) {
            return true;
        }
        if (pattern.passenger1Idx >= 0 && fixedPassengers.get(pattern.passenger1Idx)) {
            return true;
        }
        if (pattern.passenger2Idx >= 0 && fixedPassengers.get(pattern.passenger2Idx)) {
            return true;
        }
        return false;
    }

    /**
     * 计算方案的检验数
     * @param pattern 当前方案
     * @return 返回检验数
     */
    double computeReducedCost(Pattern pattern) {
        double reducedCost = pattern.aim - dualsOfRanges[pattern.driverIdx];
        if (pattern.passenger1Idx >= 0) {
            reducedCost -= dualsOfRanges[nDrivers + pattern.passenger1Idx];
        }
        if (pattern.passenger2Idx >= 0) {
            reducedCost -= dualsOfRanges[nDrivers + pattern.passenger2Idx];
        }
        return reducedCost;
    }

    /**
     * 判断子问题是否找到可加入主问题的列
     * @return 返回boolean值
     */
    boolean findNewColumns() {
        return (!patterns.isEmpty());
    }
}
