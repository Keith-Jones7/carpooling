package algo;

import java.util.Arrays;

public class KMAlgorithm {
    private static final double ZERO_THRESHOLD = 1e-9;

    private final int m;
    private final int n;
    private final double[][] weight;
    private final double[] lx;
    private final double[] ly;
    private final int[] match;
    private final boolean[] vx;
    private final boolean[] vy;
    private final double[] slack;
    boolean isTranspose = false;

    public KMAlgorithm(double[][] weight) {
        if (weight.length > weight[0].length) {
            isTranspose = true;
            this.m = weight[0].length;
            this.n = weight.length;
            this.weight = transpose(weight);
        }else {
            this.m = weight.length;
            this.n = weight[0].length;
            this.weight = weight;
        }
        this.lx = new double[m];
        this.ly = new double[n];
        this.match = new int[n];
        this.vx = new boolean[m];
        this.vy = new boolean[n];
        this.slack = new double[n];
    }

    public double[][] transpose(double[][] weight) {
        int row = weight.length, col = weight[0].length;
        double[][] matrix = new double[col][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                matrix[j][i] = weight[i][j];
            }
        }
        return matrix;
    }
    
    public int[][] transpose(int[][] match) {
        int row = match.length, col = match[0].length;
        int[][] matrix = new int[col][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                matrix[j][i] = match[i][j];
            }
        }
        return matrix;
    }
    // 初始化权重矩阵
    private void initializeLXLY() {
        for (int i = 0; i < m; i++) {
            lx[i] = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < n; j++) {
                lx[i] = Math.max(lx[i], weight[i][j]);
            }
        }
    }

    private boolean dfs(int u) {
        vx[u] = true;
        for (int v = 0; v < n; v++) {
            if (vy[v]) continue;
            double t = lx[u] + ly[v] - weight[u][v];
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
            while (true) {
                Arrays.fill(vx, false);
                Arrays.fill(vy, false);
                if (dfs(u)) break;

                double delta = Double.POSITIVE_INFINITY;
                for (int v = 0; v < n; v++) {
                    if (!vy[v]) {
                        delta = Math.min(delta, slack[v]);
                    }
                }
                for (int i = 0; i < m; i++) {
                    if (vx[i]) lx[i] -= delta;
                }
                for (int j = 0; j < n; j++) {
                    if (vy[j]) ly[j] += delta;
                    else slack[j] -= delta;
                }
            }
        }

        int[][] result = new int[m][n];
        for (int i = 0; i < n; i++) {
            if (match[i] >= 0 && weight[match[i]][i] > 0) {
                result[match[i]][i] = 1;
            }
        }
        if (isTranspose) {
            return transpose(result);
        }else {
            return result;
        }
    }
}
