package algo;

import model.Driver;
import model.Instance;
import model.Passenger;
import model.Pattern;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PricingProblem {
    Instance inst;
    int nDrivers;
    int nPassengers;
    List<Driver> driverList;
    List<Passenger> passengerList;


    double[] dualsOfRanges;
    BitSet fixedItems;
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

    void set() {

    }

    void solve(double[] dualsOfRanges, BitSet fixedItems) {
        // solve init
        this.dualsOfRanges = dualsOfRanges;
        this.fixedItems = fixedItems;
        patterns.clear();
        maxCost = 0;
        patternWithMaxCost = null;
        // solve
        solveMethod1();
    }

    // 简单遍历
    void solveMethod1() {
        for (int i = 0; i < nDrivers; i++) {

        }
    }

    // 优化遍历
    void solveMethod2() {

    }

    double computeReducedCost(Pattern pattern) {
        return 0;
    }

    boolean findNewColumns() {
        return (!patterns.isEmpty());
    }
}
