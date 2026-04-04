// GSearchTea: https://github.com/rostam/GSearchTea
// Distributed under the terms of the GNU General Public License (GPL): http://www.gnu.org/licenses/
package org;

import graphtea.extensions.G6Format;
import graphtea.extensions.algorithms.NonHomomorphism;
import graphtea.extensions.generators.CircleGenerator;
import graphtea.extensions.generators.CompleteGraphGenerator;
import graphtea.extensions.generators.GeneralizedPetersonGenerator;
import graphtea.graph.graph.GraphModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes nonhomo(G, H) for a variety of canonical graph pairs and for all
 * graphs in the bundled G6 files.  Prints results as Markdown tables.
 *
 * Run with: ./run.sh nonhomo
 */
public class NonHomoReport {

    public static void main(String[] args) throws IOException {
        System.out.println("# nonhomo Results\n");
        tableCompleteToComplete();
        tableCyclesToK2();
        tableCyclesToC5();
        tablePetersenToComplete();
        tableFromG6("all7.g6");
    }

    // -------------------------------------------------------------------------
    // Table 1: K_n → K_m
    // -------------------------------------------------------------------------
    private static void tableCompleteToComplete() {
        System.out.println("## nonhomo(K_n, K_m)\n");
        int maxN = 8;
        System.out.print("| G \\ H |");
        for (int m = 1; m <= maxN; m++) System.out.print(" K_" + m + " |");
        System.out.println();
        System.out.print("|-------|");
        for (int m = 1; m <= maxN; m++) System.out.print("-----|");
        System.out.println();
        for (int n = 1; n <= maxN; n++) {
            GraphModel G = CompleteGraphGenerator.generateCompleteGraph(n);
            System.out.print("| K_" + n + "   |");
            for (int m = 1; m <= maxN; m++) {
                if (m > n) {
                    System.out.print("  —  |");
                } else {
                    GraphModel H = CompleteGraphGenerator.generateCompleteGraph(m);
                    int val = NonHomomorphism.compute(G, H);
                    System.out.printf(" %3d |", val);
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Table 2: C_n → K_2
    // -------------------------------------------------------------------------
    private static void tableCyclesToK2() {
        System.out.println("## nonhomo(C_n, K_2)  — edges to remove to make C_n bipartite\n");
        System.out.println("| n  | nonhomo(C_n, K_2) | note                   |");
        System.out.println("|----|-------------------|------------------------|");
        GraphModel K2 = CompleteGraphGenerator.generateCompleteGraph(2);
        for (int n = 3; n <= 15; n++) {
            GraphModel Cn = CircleGenerator.generateCircle(n);
            int val = NonHomomorphism.compute(Cn, K2);
            String note = (n % 2 == 0) ? "even cycle (bipartite)" : "odd cycle";
            System.out.printf("| %2d | %17d | %-22s |%n", n, val, note);
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Table 3: C_n → C_5
    // -------------------------------------------------------------------------
    private static void tableCyclesToC5() {
        System.out.println("## nonhomo(C_n, C_5)\n");
        System.out.println("| n  | nonhomo(C_n, C_5) |");
        System.out.println("|----|-------------------|");
        GraphModel C5 = CircleGenerator.generateCircle(5);
        for (int n = 3; n <= 15; n++) {
            GraphModel Cn = CircleGenerator.generateCircle(n);
            int val = NonHomomorphism.compute(Cn, C5);
            System.out.printf("| %2d | %17d |%n", n, val);
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Table 4: Petersen graph (GP(5,2)) → K_k
    // -------------------------------------------------------------------------
    private static void tablePetersenToComplete() {
        System.out.println("## nonhomo(Petersen, K_k)\n");
        System.out.println("Petersen graph = GP(5,2): 10 vertices, 15 edges, 3-regular, " +
                "girth 5, χ=3.\n");
        System.out.println("| k | nonhomo(Petersen, K_k) |");
        System.out.println("|---|------------------------|");
        GraphModel petersen = GeneralizedPetersonGenerator.generateGeneralizedPeterson(5, 2);
        for (int k = 1; k <= 5; k++) {
            GraphModel Kk = CompleteGraphGenerator.generateCompleteGraph(k);
            int val = NonHomomorphism.compute(petersen, Kk);
            System.out.printf("| %d | %22d |%n", k, val);
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Section 5: statistics over a G6 file
    //   Targets: K_2, K_3, C_5
    //   Outputs: frequency distributions + cross-tab K_3 vs C_5 + top examples
    // -------------------------------------------------------------------------
    private static void tableFromG6(String path) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Warning: cannot read " + path + " — skipping G6 sections.");
            return;
        }

        GraphModel K2 = CompleteGraphGenerator.generateCompleteGraph(2);
        GraphModel K3 = CompleteGraphGenerator.generateCompleteGraph(3);
        GraphModel C5 = CircleGenerator.generateCircle(5);

        // Collect per-graph results
        List<int[]> results = new ArrayList<>();   // [nonhomo_K2, nonhomo_K3, nonhomo_C5]
        List<String> g6strings = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            GraphModel G = G6Format.stringToGraphModel(line);
            int nK2 = NonHomomorphism.compute(G, K2);
            int nK3 = NonHomomorphism.compute(G, K3);
            int nC5 = NonHomomorphism.compute(G, C5);
            results.add(new int[]{nK2, nK3, nC5});
            g6strings.add(line);
        }

        int total = results.size();
        System.out.println("## nonhomo distributions over `" + path + "` (" + total + " graphs)\n");

        // --- frequency distribution for each target ---
        printFreqTable(results, 0, "K_2", total);
        printFreqTable(results, 1, "K_3", total);
        printFreqTable(results, 2, "C_5", total);

        // --- cross-tabulation: nonhomo(G,K_3) vs nonhomo(G,C_5) ---
        System.out.println("### Cross-tabulation: nonhomo(G, K_3) × nonhomo(G, C_5)\n");
        System.out.println("Rows = nonhomo(G,K_3), Columns = nonhomo(G,C_5). " +
                "Entry = number of graphs.\n");

        int maxK3 = 0, maxC5 = 0;
        for (int[] r : results) { maxK3 = Math.max(maxK3, r[1]); maxC5 = Math.max(maxC5, r[2]); }
        int[][] cross = new int[maxK3 + 1][maxC5 + 1];
        for (int[] r : results) cross[r[1]][r[2]]++;

        System.out.print("| K_3 \\ C_5 |");
        for (int c = 0; c <= maxC5; c++) System.out.printf(" %3d |", c);
        System.out.println(" **row sum** |");
        System.out.print("|-----------|");
        for (int c = 0; c <= maxC5 + 1; c++) System.out.print("-----|");
        System.out.println();
        for (int r = 0; r <= maxK3; r++) {
            int rowSum = 0;
            for (int c = 0; c <= maxC5; c++) rowSum += cross[r][c];
            System.out.printf("| **%d**     |", r);
            for (int c = 0; c <= maxC5; c++) {
                if (cross[r][c] == 0) System.out.print("   — |");
                else System.out.printf(" %3d |", cross[r][c]);
            }
            System.out.printf(" %3d        |%n", rowSum);
        }
        System.out.println();
        System.out.println("The off-diagonal region (nonhomo(G,K_3)=0, nonhomo(G,C_5)>0) " +
                "identifies graphs that are 3-colourable yet not homomorphic to C_5 — " +
                "graphs whose circular chromatic number exceeds 5/2.\n");

        // --- highlight examples: largest nonhomo(G,C_5), largest nonhomo(G,K_2) ---
        System.out.println("### Notable examples\n");
        System.out.println("| G6 string | |V| | |E| | nonhomo(G,K_2) | nonhomo(G,K_3) | nonhomo(G,C_5) |");
        System.out.println("|-----------|-----|-----|----------------|----------------|----------------|");

        // Print the 10 graphs with the largest nonhomo(G,C_5), break ties by nonhomo(G,K_2) desc
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) indices.add(i);
        indices.sort((a, b) -> {
            int cmp = Integer.compare(results.get(b)[2], results.get(a)[2]);
            if (cmp != 0) return cmp;
            return Integer.compare(results.get(b)[0], results.get(a)[0]);
        });
        int shown = 0;
        for (int idx : indices) {
            if (shown >= 10) break;
            int[] r = results.get(idx);
            GraphModel G = G6Format.stringToGraphModel(g6strings.get(idx));
            System.out.printf("| %-10s | %3d | %3d | %14d | %14d | %14d |%n",
                    g6strings.get(idx),
                    G.getVerticesCount(), G.getEdgesCount(),
                    r[0], r[1], r[2]);
            shown++;
        }
        System.out.println();

        // Also show graphs in the "gap": nonhomo(G,K_3)=0 but nonhomo(G,C_5) highest
        System.out.println("#### Gap graphs: χ(G) ≤ 3 but nonhomo(G, C_5) > 0\n");
        System.out.println("(3-colourable graphs not homomorphic to C_5)\n");
        System.out.println("| G6 string | |V| | |E| | nonhomo(G,K_2) | nonhomo(G,K_3) | nonhomo(G,C_5) |");
        System.out.println("|-----------|-----|-----|----------------|----------------|----------------|");

        // Sort gap graphs by nonhomo(G,C_5) desc, then nonhomo(G,K_2) desc
        List<Integer> gapIdx = new ArrayList<>();
        for (int i = 0; i < results.size(); i++)
            if (results.get(i)[1] == 0 && results.get(i)[2] > 0) gapIdx.add(i);
        gapIdx.sort((a, b) -> {
            int cmp = Integer.compare(results.get(b)[2], results.get(a)[2]);
            if (cmp != 0) return cmp;
            return Integer.compare(results.get(b)[0], results.get(a)[0]);
        });
        int gapShown = 0;
        for (int idx : gapIdx) {
            if (gapShown >= 15) break;
            int[] r = results.get(idx);
            GraphModel G = G6Format.stringToGraphModel(g6strings.get(idx));
            System.out.printf("| %-10s | %3d | %3d | %14d | %14d | %14d |%n",
                    g6strings.get(idx),
                    G.getVerticesCount(), G.getEdgesCount(),
                    r[0], r[1], r[2]);
            gapShown++;
        }
        System.out.println("| … (%d gap graphs total) | | | | | |%n".formatted(gapIdx.size()));
        System.out.println();
    }

    private static void printFreqTable(List<int[]> results, int col,
                                        String hLabel, int total) {
        Map<Integer, Integer> freq = new TreeMap<>();
        for (int[] r : results) freq.merge(r[col], 1, Integer::sum);

        System.out.println("### nonhomo(G, " + hLabel + ") — frequency over all " +
                total + " graphs\n");
        System.out.println("| nonhomo | count | percent | bar                              |");
        System.out.println("|---------|-------|---------|----------------------------------|");
        int maxCount = freq.values().stream().mapToInt(x -> x).max().orElse(1);
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            int cnt = e.getValue();
            double pct = 100.0 * cnt / total;
            int barLen = (int) Math.round(32.0 * cnt / maxCount);
            String bar = "█".repeat(barLen);
            System.out.printf("| %7d | %5d | %5.1f%% | %-32s |%n",
                    e.getKey(), cnt, pct, bar);
        }
        System.out.println();
    }
}
