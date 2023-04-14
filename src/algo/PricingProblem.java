package algo;

import common.Param;
import model.Driver;
import model.Instance;
import model.Passenger;
import model.Pattern;

import java.util.*;

public class PricingProblem {
    Instance inst;
    int nDrivers;
    int nPassengers;
    List<Driver> driverList;
    List<Passenger> passengerList;

    ArrayList<Pattern> totalPatterns;
    double[] dualsOfRanges;
    BitSet fixedDrivers;
    BitSet fixedPassengers;
    ArrayList<Pattern> patterns;
    Pattern patternWithMaxCost;
    double maxCost;



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

    void solve(double[] dualsOfRanges, BitSet fixedDrivers, BitSet fixedPassengers) {
        // solve init
        this.dualsOfRanges = dualsOfRanges;
        this.fixedDrivers = fixedDrivers;
        this.fixedPassengers = fixedPassengers;
        patterns.clear();
        maxCost = 0;
        patternWithMaxCost = null;
        // solve
        solveMethod1();
    }

    // 简单遍历
    void solveMethod1() {
        long s0 = System.currentTimeMillis();
        int maxColNum = 100;
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
                // 更新best pattern
                if (reducedCost > maxCost + Param.EPS) {
                    maxCost = reducedCost;
                    patternWithMaxCost = pattern;
                }
            }
        }
        patterns.addAll(patternPriorityQueue);
//        totalPatterns.removeAll(patterns);
        double timeCost = Param.getTimecost(s0);
    }

    // 优化遍历
    void solveMethod2() {
        int maxColNum = 100;
        PriorityQueue<Pattern> patternPriorityQueue = new PriorityQueue<>(Comparator.comparing(o -> o.reducedCost));
        for (Pattern pattern : totalPatterns) {
            if (containFixedItem(pattern)) {
                continue;
            }
            double reducedCost = computeReducedCost(pattern);
            pattern.reducedCost = reducedCost;
            // 判断检验数大于0
            if (reducedCost > maxCost + Param.EPS) {
                // 判断是否加入优先级队列
                if (patternPriorityQueue.size() < maxColNum) {
                    patternPriorityQueue.add(pattern);
                } else {
                    if (reducedCost > patternPriorityQueue.peek().reducedCost) {
                        patternPriorityQueue.poll();
                        patternPriorityQueue.add(pattern);
                    }
                }
                // 更新best pattern
                if (reducedCost > maxCost + Param.EPS) {
                    maxCost = reducedCost;
                    patternWithMaxCost = pattern;
                }
            }
        }
        patterns.addAll(patternPriorityQueue);
        totalPatterns.removeAll(patterns);
    }

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

    boolean findNewColumns() {
        return (!patterns.isEmpty());
    }
}
