package algo;

import common.Param;
import javafx.util.Pair;
import model.Pattern;

import java.util.ArrayList;

/**
 * 松弛解类
 */

public class LPSol {
    ArrayList<Pair<Pattern, Double>> vals;    // 由二元组Pair<Pattern, Double>组成的解，第一个元素表示方案，第二个元素表示方案解值
    double objVal;   // 松弛解的目标值

    /**
     * 松弛解的构造函数
     * @param vals
     * @param objVal
     */
    public LPSol(ArrayList<Pair<Pattern, Double>> vals, double objVal) {
        this.vals = vals;
        this.objVal = objVal;
    }
}
