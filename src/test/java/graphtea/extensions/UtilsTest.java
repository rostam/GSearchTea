package graphtea.extensions;

import Jama.Matrix;
import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class UtilsTest {

    private GraphModel buildGraph(int n, int[][] edges) {
        GraphModel g = new GraphModel(false);
        Vertex[] v = new Vertex[n];
        for (int i = 0; i < n; i++) {
            v[i] = new Vertex();
            g.addVertex(v[i]);
        }
        for (int[] e : edges) {
            g.addEdge(new Edge(v[e[0]], v[e[1]]));
        }
        return g;
    }

    // --- choose ---

    @Test
    public void choose_base_cases() {
        assertEquals(BigInteger.ONE, Utils.choose(5, 0));
        assertEquals(BigInteger.ONE, Utils.choose(5, 5));
        assertEquals(BigInteger.ZERO, Utils.choose(3, 4));
        assertEquals(BigInteger.ZERO, Utils.choose(3, -1));
    }

    @Test
    public void choose_standard_values() {
        assertEquals(BigInteger.valueOf(10), Utils.choose(5, 2));
        assertEquals(BigInteger.valueOf(6),  Utils.choose(4, 2));
        assertEquals(BigInteger.valueOf(1),  Utils.choose(1, 1));
        assertEquals(BigInteger.valueOf(20), Utils.choose(6, 3));
    }

    @Test
    public void choose_symmetric() {
        // C(n,k) == C(n,n-k)
        assertEquals(Utils.choose(7, 2), Utils.choose(7, 5));
        assertEquals(Utils.choose(10, 3), Utils.choose(10, 7));
    }

    // --- getMaxDegree ---

    @Test
    public void getMaxDegree_K3() {
        // K3: every vertex has degree 2
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        assertEquals(2, Utils.getMaxDegree(g));
    }

    @Test
    public void getMaxDegree_star() {
        // Star K1,3: centre has degree 3, leaves have degree 1
        GraphModel g = buildGraph(4, new int[][]{{0,1},{0,2},{0,3}});
        assertEquals(3, Utils.getMaxDegree(g));
    }

    @Test
    public void getMaxDegree_noEdges() {
        GraphModel g = buildGraph(3, new int[][]{});
        assertEquals(0, Utils.getMaxDegree(g));
    }

    // --- getDegreeSum ---

    @Test
    public void getDegreeSum_alpha1_sumOfNeighborDegrees() {
        // getDegreeSum(g, alpha) = sum_v sum_{u~v} deg(u)^alpha
        // P4 (0-1-2-3), degrees 1,2,2,1, alpha=1:
        //   v0: deg(1)=2              → 2
        //   v1: deg(0)+deg(2)=1+2     → 3
        //   v2: deg(1)+deg(3)=2+1     → 3
        //   v3: deg(2)=2              → 2  → total = 10
        GraphModel g = buildGraph(4, new int[][]{{0,1},{1,2},{2,3}});
        assertEquals(10.0, Utils.getDegreeSum(g, 1.0), 1e-9);
    }

    @Test
    public void getDegreeSum_alpha0_equalsNumVertices() {
        // deg(v)^0 = 1 for every vertex with at least one edge... but isolated
        // vertices have degree 0 and 0^0 = 1 in this context (Math.pow(0,0)=1)
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        // Each vertex sums its neighbours' degree^0 = num_neighbours * 1
        // vertex 0: 2 neighbours, vertex 1: 2, vertex 2: 2 → total = 6
        assertEquals(6.0, Utils.getDegreeSum(g, 0.0), 1e-9);
    }

    @Test
    public void getDegreeSum_returnsDoubleNotTruncated() {
        // alpha=0.5: confirm the result is fractional (not silently cast to int)
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        double result = Utils.getDegreeSum(g, 0.5);
        // Each vertex has degree 2; each sums sqrt(2) per neighbour * 2 neighbours
        // total = 3 * 2 * sqrt(2) = 6*sqrt(2) ≈ 8.485
        assertEquals(6 * Math.sqrt(2), result, 1e-9);
    }

    // --- getMinNonPendentDegree ---

    @Test
    public void getMinNonPendentDegree_skipsDegree1() {
        // Star: one degree-3 centre, three degree-1 leaves → min non-pendent = 3
        GraphModel g = buildGraph(4, new int[][]{{0,1},{0,2},{0,3}});
        assertEquals(3.0, Utils.getMinNonPendentDegree(g), 1e-9);
    }

    @Test
    public void getMinNonPendentDegree_noLeaves() {
        // K3: all degree 2 → min = 2
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        assertEquals(2.0, Utils.getMinNonPendentDegree(g), 1e-9);
    }

    // --- getBinaryPattern ---

    @Test
    public void getBinaryPattern_zeroStaysZero() {
        double[][] mat = {{0, 0}, {0, 0}};
        int[][] bin = Utils.getBinaryPattern(mat, 2);
        assertArrayEquals(new int[]{0, 0}, bin[0]);
        assertArrayEquals(new int[]{0, 0}, bin[1]);
    }

    @Test
    public void getBinaryPattern_nonzeroBecomesOne() {
        double[][] mat = {{0, 3.5}, {-1, 0}};
        int[][] bin = Utils.getBinaryPattern(mat, 2);
        assertEquals(0, bin[0][0]);
        assertEquals(1, bin[0][1]);
        assertEquals(1, bin[1][0]);
        assertEquals(0, bin[1][1]);
    }

    // --- getLaplacian ---

    @Test
    public void getLaplacian_K3_rowSumsZero() {
        // For any graph Laplacian, every row sums to 0
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        Matrix A = g.getAdjacencyMatrix();
        Matrix L = Utils.getLaplacian(A);
        double[][] la = L.getArray();
        for (double[] row : la) {
            double sum = 0;
            for (double v : row) sum += v;
            assertEquals(0.0, sum, 1e-9);
        }
    }

    @Test
    public void getLaplacian_K3_diagonalIsDegree() {
        // K3: every vertex has degree 2, so diagonal of L should be 2
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        Matrix A = g.getAdjacencyMatrix();
        Matrix L = Utils.getLaplacian(A);
        double[][] la = L.getArray();
        assertEquals(2.0, la[0][0], 1e-9);
        assertEquals(2.0, la[1][1], 1e-9);
        assertEquals(2.0, la[2][2], 1e-9);
    }

    @Test
    public void getLaplacian_offDiagonalIsNegativeAdjacency() {
        // For K3, L[i][j] = -1 for i≠j (since all pairs connected)
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        Matrix A = g.getAdjacencyMatrix();
        Matrix L = Utils.getLaplacian(A);
        double[][] la = L.getArray();
        assertEquals(-1.0, la[0][1], 1e-9);
        assertEquals(-1.0, la[0][2], 1e-9);
        assertEquals(-1.0, la[1][2], 1e-9);
    }
}
