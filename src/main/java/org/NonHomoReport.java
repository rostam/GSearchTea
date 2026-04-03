// GSearchTea: https://github.com/rostam/GSearchTea
// Distributed under the terms of the GNU General Public License (GPL): http://www.gnu.org/licenses/
package org;

import graphtea.extensions.algorithms.NonHomomorphism;
import graphtea.extensions.generators.CircleGenerator;
import graphtea.extensions.generators.CompleteGraphGenerator;
import graphtea.extensions.generators.GeneralizedPetersonGenerator;
import graphtea.extensions.generators.KmnGenerator;
import graphtea.extensions.generators.PathGenerator;
import graphtea.graph.graph.GraphModel;

/**
 * Computes nonhomo(G, H) for a variety of canonical graph pairs and prints
 * the results as Markdown tables.
 *
 * Run with: ./run.sh nonhomo
 */
public class NonHomoReport {

    public static void main(String[] args) {
        System.out.println("# nonhomo Results\n");
        tableCompleteToComplete();
        tableCyclesToK2();
        tableCyclesToC5();
        tablePetersenToComplete();
        tableKmnToK2();
    }

    // -------------------------------------------------------------------------
    // Table 1: K_n → K_m
    // -------------------------------------------------------------------------
    private static void tableCompleteToComplete() {
        System.out.println("## nonhomo(K_n, K_m)\n");
        int maxN = 8;
        // Header
        System.out.print("| G \\ H |");
        for (int m = 1; m <= maxN; m++) System.out.print(" K_" + m + " |");
        System.out.println();
        System.out.print("|-------|");
        for (int m = 1; m <= maxN; m++) System.out.print("-----|");
        System.out.println();
        // Rows
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
    // Table 2: C_n → K_2  (bipartite edge deletion number)
    // -------------------------------------------------------------------------
    private static void tableCyclesToK2() {
        System.out.println("## nonhomo(C_n, K_2)  — edges to remove to make C_n bipartite\n");
        System.out.println("| n  | nonhomo(C_n, K_2) | note           |");
        System.out.println("|----|-------------------|----------------|");
        GraphModel K2 = CompleteGraphGenerator.generateCompleteGraph(2);
        for (int n = 3; n <= 15; n++) {
            GraphModel Cn = CircleGenerator.generateCircle(n);
            int val = NonHomomorphism.compute(Cn, K2);
            String note = (n % 2 == 0) ? "even cycle (bipartite)" : "odd cycle";
            System.out.printf("| %2d | %17d | %-14s |%n", n, val, note);
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
        System.out.println("The Petersen graph is the generalised Petersen graph GP(5,2): " +
                "10 vertices, 15 edges, 3-regular, girth 5, chromatic number 3.\n");
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
    // Table 5: K_{m,n} → K_2  (always 0: complete bipartite is bipartite)
    //          and K_{m,n} → K_3
    // -------------------------------------------------------------------------
    private static void tableKmnToK2() {
        System.out.println("## nonhomo(K_{m,n}, K_k) for small m, n\n");
        System.out.println("| G         | nonhomo(G, K_2) | nonhomo(G, K_3) |");
        System.out.println("|-----------|-----------------|-----------------|");
        GraphModel K2 = CompleteGraphGenerator.generateCompleteGraph(2);
        GraphModel K3 = CompleteGraphGenerator.generateCompleteGraph(3);
        int[][] pairs = {{1,1},{1,2},{1,3},{1,4},{2,2},{2,3},{2,4},{3,3}};
        for (int[] p : pairs) {
            int mm = p[0], nn = p[1];
            GraphModel G = KmnGenerator.generateKmn(mm, nn);
            int v2 = NonHomomorphism.compute(G, K2);
            int v3 = NonHomomorphism.compute(G, K3);
            System.out.printf("| K_{%d,%d}    | %15d | %15d |%n", mm, nn, v2, v3);
        }
        System.out.println();
    }
}
