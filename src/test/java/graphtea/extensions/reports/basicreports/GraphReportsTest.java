package graphtea.extensions.reports.basicreports;

import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;
import graphtea.library.algorithms.util.BipartiteChecker;
import org.junit.Test;

import java.util.ArrayList;

import static graphtea.extensions.reports.basicreports.NumOfConnectedComponents.getConnectedComponents;
import static graphtea.extensions.reports.basicreports.NumOfTriangles.getNumOfTriangles;
import static org.junit.Assert.*;

public class GraphReportsTest {

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

    // --- IsBipartite ---

    @Test
    public void bipartite_tree() {
        GraphModel g = buildGraph(4, new int[][]{{0,1},{1,2},{2,3}});
        assertTrue(BipartiteChecker.isBipartite(g));
    }

    @Test
    public void bipartite_evenCycle() {
        GraphModel g = buildGraph(4, new int[][]{{0,1},{1,2},{2,3},{3,0}});
        assertTrue(BipartiteChecker.isBipartite(g));
    }

    @Test
    public void notBipartite_triangle() {
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2},{0,2}});
        assertFalse(BipartiteChecker.isBipartite(g));
    }

    @Test
    public void notBipartite_oddCycle() {
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{2,3},{3,4},{4,0}});
        assertFalse(BipartiteChecker.isBipartite(g));
    }

    // --- IsEulerian ---

    @Test
    public void eulerian_evenDegrees() {
        // C4: every vertex has degree 2 → Eulerian
        GraphModel g = buildGraph(4, new int[][]{{0,1},{1,2},{2,3},{3,0}});
        IsEulerian report = new IsEulerian();
        assertEquals(true, report.calculate(g));
    }

    @Test
    public void notEulerian_oddDegree() {
        // Path P3: endpoints have degree 1 → not Eulerian
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2}});
        IsEulerian report = new IsEulerian();
        assertEquals(false, report.calculate(g));
    }

    @Test
    public void eulerian_K3() {
        // K3: all degree 2 → Eulerian
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2},{0,2}});
        IsEulerian report = new IsEulerian();
        assertEquals(true, report.calculate(g));
    }

    @Test
    public void notEulerian_disconnected() {
        // Two isolated vertices — not connected → not Eulerian
        GraphModel g = buildGraph(2, new int[][]{});
        IsEulerian report = new IsEulerian();
        assertEquals(false, report.calculate(g));
    }

    // --- NumOfTriangles ---

    @Test
    public void triangles_K3_one() {
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2},{0,2}});
        assertEquals(1, getNumOfTriangles(g));
    }

    @Test
    public void triangles_K4_four() {
        // K4 has C(4,3) = 4 triangles
        GraphModel g = buildGraph(4, new int[][]{{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}});
        assertEquals(4, getNumOfTriangles(g));
    }

    @Test
    public void triangles_tree_zero() {
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{2,3},{3,4}});
        assertEquals(0, getNumOfTriangles(g));
    }

    @Test
    public void triangles_cycle_zero() {
        // C5 has no triangles
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{2,3},{3,4},{4,0}});
        assertEquals(0, getNumOfTriangles(g));
    }

    // --- NumOfConnectedComponents ---

    @Test
    public void components_connected_one() {
        GraphModel g = buildGraph(3, new int[][]{{0,1},{1,2}});
        assertEquals(1, getConnectedComponents(g).size());
    }

    @Test
    public void components_isolated_each_own() {
        // 3 isolated vertices → 3 components
        GraphModel g = buildGraph(3, new int[][]{});
        assertEquals(3, getConnectedComponents(g).size());
    }

    @Test
    public void components_two_parts() {
        // Vertices 0-1 connected, vertex 2 isolated → 2 components
        GraphModel g = buildGraph(3, new int[][]{{0,1}});
        ArrayList<ArrayList<Integer>> comps = getConnectedComponents(g);
        assertEquals(2, comps.size());
    }

    @Test
    public void components_sizes_correct() {
        // 0-1-2 connected, 3-4 connected → components of size 3 and 2
        GraphModel g = buildGraph(5, new int[][]{{0,1},{1,2},{3,4}});
        ArrayList<ArrayList<Integer>> comps = getConnectedComponents(g);
        assertEquals(2, comps.size());
        int big = Math.max(comps.get(0).size(), comps.get(1).size());
        int small = Math.min(comps.get(0).size(), comps.get(1).size());
        assertEquals(3, big);
        assertEquals(2, small);
    }
}
