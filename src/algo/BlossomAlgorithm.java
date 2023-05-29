package algo;

import common.Param;

import java.util.ArrayList;
import java.util.Arrays;

// todo 结果正确性存疑
public class BlossomAlgorithm {
    int n;
    ArrayList<Integer>[] graph;
    int[] match, p, base, q;
    boolean[] used, blossom;
    ArrayList<Integer> path;

    public BlossomAlgorithm(double[][] costMatrix) {
        n = costMatrix.length;
        graph = createGraph(copyMatrix(costMatrix));
        match = new int[n];
        p = new int[n];
        base = new int[n];
        q = new int[n];
        used = new boolean[n];
        blossom = new boolean[n];
    }

    double[][] copyMatrix(double[][] costMatrix) {
        int len = costMatrix.length;
        double[][] copy = new double[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = i + 1; j < len; j++) {
                copy[i][j] = Math.max(costMatrix[i][j], costMatrix[j][i]);
                copy[j][i] = copy[i][j];
            }
        }
        return copy;
    }

    ArrayList<Integer>[] createGraph(double[][] costMatrix) {
        ArrayList<Integer>[] graph = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            graph[i] = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                // 更改阈值以考虑浮点数精度问题
                if (costMatrix[i][j] > Param.EPS) {
                    graph[i].add(j);
                }
            }
        }
        return graph;
    }

    int lca(int a, int b) {
        Arrays.fill(blossom, false);
        while (true) {
            a = base[a];
            blossom[a] = true;
            if (match[a] == -1) break;
            a = p[match[a]];
        }

        while (true) {
            b = base[b];
            if (blossom[b]) return b;
            b = p[match[b]];
        }
    }

    void markPath(int v, int b, int children) {
        while (base[v] != b) {
            blossom[base[v]] = blossom[base[match[v]]] = true;
            p[v] = children;
            children = match[v];
            v = p[match[v]];
        }
    }

    int findPath(int root) {
        Arrays.fill(used, false);
        Arrays.fill(p, -1);
        for (int i = 0; i < n; ++i) {
            base[i] = i;
        }

        used[root] = true;
        int qh = 0;
        int qt = 0;
        q[qt++] = root;
        while (qh < qt) {
            int v = q[qh++];
            for (int to : graph[v]) {
                if (base[v] == base[to] || match[v] == to) continue;
                if (to == root || (match[to] != -1 && p[match[to]] != -1)) {
                    int curbase = lca(v, to);
                    Arrays.fill(blossom, false);
                    markPath(v, curbase, to);
                    markPath(to, curbase, v);
                    for (int i = 0; i < n; ++i) {
                        if (blossom[base[i]]) {
                            base[i] = curbase;
                            if (!used[i]) {
                                used[i] = true;
                                q[qt++] = i;
                            }
                        }
                    }
                } else if (p[to] == -1) {
                    p[to] = v;
                    if (match[to] == -1) {
                        return to;
                    }
                    to = match[to];
                    used[to] = true;
                    q[qt++] = to;
                }
            }
        }
        return -1;
    }

    void getMaximalMatching() {
        Arrays.fill(match, -1);
        for (int i = 0; i < n; ++i) {
            if (match[i] == -1) {
                int v = findPath(i);
                while (v != -1) {
                    int pv = p[v], ppv = match[p[v]];
                    match[v] = pv;
                    match[pv] = v;
                    v = ppv;
                }
            }
        }
    }

    public int[][] generateResultMatrix() {
        getMaximalMatching();
        int[][] resultMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            if (i < match[i]) { // 避免重复，只处理匹配值大于当前索引的情况
                resultMatrix[i][match[i]] = 1;
                resultMatrix[match[i]][i] = 1;
            }
        }
        return resultMatrix;
    }
}
