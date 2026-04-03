// GSearchTea: https://github.com/rostam/GSearchTea
// Distributed under the terms of the GNU General Public License (GPL): http://www.gnu.org/licenses/
package graphtea.extensions.algorithms;

import graphtea.graph.graph.GraphModel;

/**
 * Computes the non-homomorphism distance between two graphs.
 *
 * <p>nonhomo(G, H) is defined as the minimum number of edges that must be
 * removed from G so that the resulting graph admits a homomorphism to H.
 *
 * <p>Equivalently:
 * <pre>
 *   nonhomo(G, H) = min_{f : V(G) → V(H)}  |{ (u,v) ∈ E(G) : (f(u), f(v)) ∉ E(H) }|
 * </pre>
 *
 * <p>Special cases:
 * <ul>
 *   <li>nonhomo(G, K_k) = minimum edges to remove to make G k-colourable.</li>
 *   <li>nonhomo(G, K_2) = minimum edges to remove to make G bipartite
 *       (the bipartite edge deletion number).</li>
 *   <li>nonhomo(G, H) = 0 iff G is already homomorphic to H.</li>
 * </ul>
 *
 * <p>The algorithm enumerates all |V(H)|^|V(G)| mappings with branch-and-bound
 * pruning: once accumulated bad edges reach the current best, that branch
 * is discarded.
 */
public class NonHomomorphism {

    /**
     * Computes nonhomo(G, H).
     *
     * @param G source graph (edges may be removed)
     * @param H target graph (unchanged)
     * @return minimum number of edges to remove from G so it becomes
     *         homomorphic to H; returns 0 when G is already homomorphic to H.
     */
    public static int compute(GraphModel G, GraphModel H) {
        int n = G.getVerticesCount();
        int m = H.getVerticesCount();

        if (m == 0) {
            // No vertices in H; only the empty graph maps to it.
            return G.getEdgesCount();
        }
        if (n == 0) {
            return 0;
        }

        // Build boolean adjacency matrices from Jama double[][] arrays.
        double[][] rawG = G.getAdjacencyMatrix().getArray();
        double[][] rawH = H.getAdjacencyMatrix().getArray();

        boolean[][] adjH = new boolean[m][m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < m; j++)
                adjH[i][j] = rawH[i][j] > 0;

        // Build edge list for G (undirected: store each edge once, src < dst).
        int edgeCount = 0;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                if (rawG[i][j] > 0) edgeCount++;

        int[] edgeSrc = new int[edgeCount];
        int[] edgeDst = new int[edgeCount];
        int idx = 0;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                if (rawG[i][j] > 0) {
                    edgeSrc[idx] = i;
                    edgeDst[idx] = j;
                    idx++;
                }

        // For fast lookup: for each vertex v in G, which edge indices have
        // edgeDst[k] == v (i.e., backward edges when we assign vertex v)?
        // This lets us count only the newly-determined bad edges at each step.
        @SuppressWarnings("unchecked")
        java.util.List<Integer>[] backEdges = new java.util.ArrayList[n];
        for (int v = 0; v < n; v++) backEdges[v] = new java.util.ArrayList<>();
        for (int k = 0; k < edgeCount; k++)
            backEdges[edgeDst[k]].add(k);

        int[] mapping = new int[n];
        int[] minBad = {edgeCount}; // upper bound: remove every edge

        enumerate(0, n, m, mapping, edgeSrc, edgeDst, backEdges, adjH, minBad, 0);

        return minBad[0];
    }

    /**
     * Recursive branch-and-bound enumeration of vertex mappings.
     *
     * @param pos        index of vertex currently being assigned
     * @param n          |V(G)|
     * @param m          |V(H)|
     * @param mapping    partial mapping built so far (indices 0..pos-1 are set)
     * @param edgeSrc    source vertex of each edge (src < dst)
     * @param edgeDst    destination vertex of each edge
     * @param backEdges  backEdges[v] = list of edge indices k where edgeDst[k]==v
     * @param adjH       adjacency matrix of H
     * @param minBad     best (minimum) bad-edge count found so far (single-element array)
     * @param currentBad number of bad edges accumulated so far (for assigned vertices)
     */
    private static void enumerate(int pos, int n, int m, int[] mapping,
                                  int[] edgeSrc, int[] edgeDst,
                                  java.util.List<Integer>[] backEdges,
                                  boolean[][] adjH,
                                  int[] minBad, int currentBad) {
        if (currentBad >= minBad[0]) return; // Prune: can't improve
        if (pos == n) {
            minBad[0] = currentBad;           // New best found
            return;
        }
        for (int c = 0; c < m; c++) {
            mapping[pos] = c;
            // Count new bad edges: those with edgeDst[k]==pos (backward edges),
            // so their source vertex was assigned earlier.
            int newBad = 0;
            for (int k : backEdges[pos]) {
                if (!adjH[mapping[edgeSrc[k]]][c]) {
                    newBad++;
                }
            }
            enumerate(pos + 1, n, m, mapping, edgeSrc, edgeDst, backEdges,
                      adjH, minBad, currentBad + newBad);
        }
    }
}
