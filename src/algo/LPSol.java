package algo;

import common.Param;
import javafx.util.Pair;
import model.Pattern;

import java.util.ArrayList;


public class LPSol {
    ArrayList<Pair<Pattern, Double>> vals;
    double objVal;

    public LPSol(ArrayList<Pair<Pattern, Double>> vals, double objVal) {
        this.vals = vals;
        this.objVal = objVal;
    }

    public LPSol copy() {
        return null;
    }

    public boolean isIntegral() {
        for (Pair<Pattern, Double> pair : vals) {
            if (!Param.isInt(pair.getValue())) {
                return false;
            }
        }
        return true;
    }
}
