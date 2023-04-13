package algo;

import common.Param;

import java.util.ArrayList;
import java.util.Arrays;

public class KMAlgorithm {
    private static final double ZERO_THRESHOLD = Param.EPS;

    private final int m;
    private final int n;
    private final ArrayList<ArrayList<Edge>> graph;
    private final double[] lx;
    private final double[] ly;
    private final int[] match;
    private final boolean[] vx;
    private final boolean[] vy;
    private final double[] slack;
    private final boolean transpose;

    public KMAlgorithm(double[][] weight) {
        if (weight.length > weight[0].length) {
            transpose = true;
            int rowNum = weight[0].length;
            int colNum = weight.length;
            double[][] weightT = new double[rowNum][colNum];
            for (int i = 0; i < rowNum; i++) {
                for (int j = 0; j < colNum; j++) {
                    weightT[i][j] = weight[j][i];
                }
            }
            weight = weightT;
        } else {
            transpose = false;
        }
        this.m = weight.length;
        this.n = weight[0].length;
        this.graph = new ArrayList<>(m);
        this.lx = new double[m];
        this.ly = new double[n];
        this.match = new int[n];
        this.vx = new boolean[m];
        this.vy = new boolean[n];
        this.slack = new double[n];

        // 将矩阵转为邻接表表示
        for (int i = 0; i < m; i++) {
            ArrayList<Edge> edges = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (weight[i][j] > 0) {
                    edges.add(new Edge(j, weight[i][j]));
                }
            }
            graph.add(edges);
        }
    }
    private boolean checkLxChange(double[] lxOld, double[] lx) {
        for (int i = 0; i < lx.length; i++) {
            if(Math.abs(lxOld[i] - lx[i]) > ZERO_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
    private static class Edge {
        int v;
        double w;

        public Edge(int v, double w) {
            this.v = v;
            this.w = w;
        }
    }

    // 初始化权重矩阵
    private void initializeLXLY() {
        for (int i = 0; i < m; i++) {
            lx[i] = Double.NEGATIVE_INFINITY;
            for (Edge edge : graph.get(i)) {
                lx[i] = Math.max(lx[i], edge.w);
            }
        }
    }

    private boolean dfs(int u) {
        vx[u] = true;
        for (Edge edge : graph.get(u)) {
            int v = edge.v;
            if (vy[v]) continue;
            double t = lx[u] + ly[v] - edge.w;
            if (Math.abs(t) < ZERO_THRESHOLD) {
                vy[v] = true;
                if (match[v] == -1 || dfs(match[v])) {
                    match[v] = u;
                    return true;
                }
            } else {
                slack[v] = Math.min(slack[v], t);
            }
        }
        return false;
    }

    public int[][] getMatch() {
        initializeLXLY();
        Arrays.fill(match, -1);

        for (int u = 0; u < m; u++) {
            Arrays.fill(slack, Double.POSITIVE_INFINITY);
            boolean lxChanged = true;
            while (lxChanged) {
                Arrays.fill(vx, false);
                Arrays.fill(vy, false);
                if (dfs(u)) break;

                double delta = Double.POSITIVE_INFINITY;
                for (int v = 0; v < n; v++) {
                    if (!vy[v]) {
                        delta = Math.min(delta, slack[v]);
                    }
                }
                double[] oldLx = Arrays.copyOf(lx, m);
                for (int i = 0; i < m;
                     i++) {
                    if (vx[i]) lx[i] -= delta;
                }
                for (int j = 0; j < n; j++) {
                    if (vy[j]) ly[j] += delta;
                    else slack[j] -= delta;
                }
                lxChanged = checkLxChange(oldLx, lx);
            }
        }
        int[][] result;
        if (transpose) {
            result = new int[n][m];
        }else {
            result = new int[m][n];
        }
        for (int j = 0; j < n; j++) {
            final int finalJ = j;
            if (match[j] >= 0 && graph.get(match[j]).stream().anyMatch(edge -> edge.v == finalJ)) {
                result[transpose ? j : match[j]][transpose ? match[j] : j] = 1;
            }
        }
        return result;
    }

}
