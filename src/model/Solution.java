package model;

import java.util.*;
public class Solution {
    public double profit;
    public ArrayList<Pattern> patterns;

    public Solution() {
        this.patterns = new ArrayList<>();
    }
    public Solution(ArrayList<Pattern> patterns, double profit) {
        this.patterns = patterns;
        this.profit = profit;
    }
}
