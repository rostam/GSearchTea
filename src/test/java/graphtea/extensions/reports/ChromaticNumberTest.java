package graphtea.extensions.reports;

import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChromaticNumberTest {

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

    @Test
    public void singleVertex_chi1() {
        GraphModel g = buildGraph(1, new int[][]{});
        assertEquals(1, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void singleEdge_chi2() {
        GraphModel g = buildGraph(2, new int[][]{{0, 1}});
        assertEquals(2, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void tree_alwaysChi2() {
        // Path P5 is bipartite → χ = 2
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{2,3},{3,4}});
        assertEquals(2, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void evenCycle_chi2() {
        // C4 is bipartite → χ = 2
        GraphModel g = buildGraph(4, new int[][]{{0,1},{1,2},{2,3},{3,0}});
        assertEquals(2, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void oddCycle_chi3() {
        // C5 (odd cycle) → χ = 3
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{2,3},{3,4},{4,0}});
        assertEquals(3, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void triangle_chi3() {
        // K3 → χ = 3
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2},{0,2}});
        assertEquals(3, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void K4_chi4() {
        GraphModel g = buildGraph(4, new int[][]{{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}});
        assertEquals(4, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void K5_chi5() {
        GraphModel g = buildGraph(5, new int[][]{
            {0,1},{0,2},{0,3},{0,4},
            {1,2},{1,3},{1,4},
            {2,3},{2,4},
            {3,4}
        });
        assertEquals(5, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void completeBipartite_K33_chi2() {
        // K3,3 is bipartite → χ = 2
        GraphModel g = buildGraph(6, new int[][]{
            {0,3},{0,4},{0,5},
            {1,3},{1,4},{1,5},
            {2,3},{2,4},{2,5}
        });
        assertEquals(2, ChromaticNumber.getChromaticNumber(g));
    }

    @Test
    public void wheelGraph_W5_chi4() {
        // W5: hub + C5. Hub is adjacent to all; C5 is odd → χ = 4
        GraphModel g = buildGraph(6, new int[][]{
            {0,1},{0,2},{0,3},{0,4},{0,5},  // hub to rim
            {1,2},{2,3},{3,4},{4,5},{5,1}   // C5 rim
        });
        assertEquals(4, ChromaticNumber.getChromaticNumber(g));
    }
}
