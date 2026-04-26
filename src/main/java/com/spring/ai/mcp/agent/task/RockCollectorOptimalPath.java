package com.spring.ai.mcp.agent.task;

import java.util.Arrays;
import java.util.function.Function;

public class RockCollectorOptimalPath {

    /*
    You are an avid rock collector who lives in southern California. Some rare and desirable rocks just became available in New York, so you are planning a cross-country road trip. There are several other rare rocks that you could pick up along the way. You have been given a grid filled with numbers, representing the number of rare rocks available in various cities across the country. Your objective is to find the optimal path from So_Cal to New_York that would allow you to accumulate the most rocks along the way.

    Note: You can only travel either north (up) or east (right).
    b) Consider adding some additional tests in doTestsPass().
    c) Implement optimalPath() correctly.
    d) Here is an example:
    ^
    {{0, 0, 0, 0, 5}, New_York (finish) N
    {0, 1, 1, 1, 0},
    So_Cal (start) {2, 0, 0, 0, 0}} S
    v
    The total for this example would be 10 (2+0+1+1+1+0+5).
     */

    /**
     * Weighted-tree approach: create a Node per cell (with right and up pointers)
     * and compute the maximum path sum using recursion + per-node memoization.
     *
     * Performance: O(R*C) time, O(R*C) space (Node objects + pointers + Integer caches).
     */
    private static int optimalPathWeightedTree(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return 0;
        int rows = grid.length;
        int cols = grid[0].length;

        class Node {
            int val;
            Node right;
            Node up;
            Integer cache;

            Node(int v) { this.val = v; }
        }

        Node[][] nodes = new Node[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) nodes[i][j] = new Node(grid[i][j]);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (j + 1 < cols) nodes[i][j].right = nodes[i][j + 1];
                if (i - 1 >= 0) nodes[i][j].up = nodes[i - 1][j];
            }
        }

        Function<Node, Integer> dfsNode = new Function<>() {
            @Override
            public Integer apply(Node node) {
                if (node == null) return Integer.MIN_VALUE / 4;
                if (node.cache != null) return node.cache;
                if (node.up == null && node.right == null) { // single cell grid
                    node.cache = node.val;
                    return node.cache;
                }
                int right = apply(node.right);
                int up = apply(node.up);
                int best = Math.max(right, up);
                if (best == Integer.MIN_VALUE / 4) {
                    node.cache = Integer.MIN_VALUE / 4;
                } else {
                    node.cache = node.val + best;
                }
                return node.cache;
            }
        };

        int res = dfsNode.apply(nodes[rows - 1][0]);
        return res == Integer.MIN_VALUE / 4 ? 0 : res;
    }

    /**
     * Memoized DFS using primitive int[][] memo to cache computed cells. Avoids per-cell
     * object allocation and boxing.
     *
     * Performance: O(R*C) time, O(R*C) space (primitive memo array), recursion depth O(R+C).
     */
    private static int optimalPathMemoizedDFS(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return 0;
        int rows = grid.length;
        int cols = grid[0].length;
        final int NEG = Integer.MIN_VALUE / 4;
        int[][] memo = new int[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) memo[i][j] = NEG;
        int result = dfsMemo(grid, rows - 1, 0, memo, NEG);
        return result == NEG ? 0 : result;
    }

    private static int dfsMemo(int[][] grid, int r, int c, int[][] memo, final int NEG) {
        int cols = grid[0].length;
        if (r < 0 || c >= cols) return NEG;
        if (r == 0 && c == cols - 1) return grid[0][cols - 1];
        if (memo[r][c] != NEG) return memo[r][c];
        int right = dfsMemo(grid, r, c + 1, memo, NEG);
        int up = dfsMemo(grid, r - 1, c, memo, NEG);
        int best = Math.max(right, up);
        if (best == NEG) memo[r][c] = NEG;
        else memo[r][c] = grid[r][c] + best;
        return memo[r][c];
    }

    /**
     * Iterative 1D DP (space-optimized). Uses a single int[] of length cols and updates
     * it row-by-row. This is the most memory- and time-efficient approach in Java.
     *
     * Performance: O(R*C) time, O(C) space.
     */
    private static int optimalPathIterative1D(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return 0;
        int rows = grid.length;
        int cols = grid[0].length;
        final int NEG = Integer.MIN_VALUE / 4;
        int[] dp = new int[cols];
        Arrays.fill(dp, NEG);

        for (int r = 0; r < rows; r++) {
            for (int c = cols - 1; c >= 0; c--) {
                if (r == 0 && c == cols - 1) {
                    dp[c] = grid[0][cols - 1];
                } else {
                    int up = (r - 1 >= 0) ? dp[c] : NEG; // dp[c] still holds previous row's value
                    int right = (c + 1 < cols) ? dp[c + 1] : NEG; // dp[c+1] holds current row's value
                    int best = Math.max(up, right);
                    dp[c] = (best == NEG) ? NEG : grid[r][c] + best;
                }
            }
        }
        return dp[0] == NEG ? 0 : dp[0];
    }

    // Additional test cases validating all three implementations
    public static void doTestsPass() {
        int[][] grid1 = {
                {0, 0, 0, 0, 5},
                {0, 1, 1, 1, 0},
                {2, 0, 0, 0, 0}
        };

        int expected1 = 10;
        System.out.println("grid1 weighted-tree: " + (optimalPathWeightedTree(grid1) == expected1));
        System.out.println("grid1 memoized-dfs: " + (optimalPathMemoizedDFS(grid1) == expected1));
        System.out.println("grid1 iterative-1d: " + (optimalPathIterative1D(grid1) == expected1));

        int[][] grid2 = {
                {0, 0, 0, 0, 10},
                {0, 1, 1, 1, 1},
                {1, 0, 0, 0, 0}
        };

        int expected2 = 15;
        System.out.println("grid2 weighted-tree: " + (optimalPathWeightedTree(grid2) == expected2));
        System.out.println("grid2 memoized-dfs: " + (optimalPathMemoizedDFS(grid2) == expected2));
        System.out.println("grid2 iterative-1d: " + (optimalPathIterative1D(grid2) == expected2));
    }

    public static void main(String[] args) {
        doTestsPass();
    }

}
