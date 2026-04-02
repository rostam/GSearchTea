package graphtea.extensions;

import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;
import org.junit.Test;

import java.util.Vector;
import java.util.HashMap;

import static org.junit.Assert.*;

public class G6FormatTest {

    // Helper: build an undirected GraphModel from an edge list.
    // Vertices are indexed 0..n-1.
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

    // --- graphsize ---

    @Test
    public void graphsize_singleVertex() {
        // n=1: char('@') = 64 = 1+63
        String g6 = "@";
        assertEquals(1, G6Format.graphsize(g6));
    }

    @Test
    public void graphsize_threeVertices() {
        // 'B' = 66 = 3+63
        assertEquals(3, G6Format.graphsize("Bw"));
    }

    @Test
    public void graphsize_stripsLeadingColon() {
        // sparse6 prefix ':' should be stripped
        assertEquals(3, G6Format.graphsize(":Bw"));
    }

    // --- stringToGraph ---

    @Test
    public void stringToGraph_K3_hasAllEdges() {
        // K3 (triangle): g6 = "Bw"
        // edges: (0,1), (0,2), (1,2)
        HashMap<Integer, Vector<Integer>> adj = G6Format.stringToGraph("Bw");
        assertTrue(adj.containsKey(0));
        assertTrue(adj.get(0).contains(1));
        assertTrue(adj.get(0).contains(2));
        assertTrue(adj.containsKey(1));
        assertTrue(adj.get(1).contains(2));
    }

    @Test
    public void stringToGraph_emptyGraph_noEdges() {
        // n=3, no edges: bits "000" → padded "000000" → 0+63=63='?'  → "B?"
        HashMap<Integer, Vector<Integer>> adj = G6Format.stringToGraph("B?");
        assertTrue(adj.isEmpty());
    }

    @Test
    public void stringToGraph_singleEdge() {
        // n=2, edge (0,1): bits "1" → padded "100000" → 32+63=95='_' → "A_"
        HashMap<Integer, Vector<Integer>> adj = G6Format.stringToGraph("A_");
        assertTrue(adj.containsKey(0));
        assertTrue(adj.get(0).contains(1));
    }

    // --- stringToGraphModel ---

    @Test
    public void stringToGraphModel_K3_vertexAndEdgeCount() {
        GraphModel g = G6Format.stringToGraphModel("Bw");
        assertEquals(3, g.numOfVertices());
        assertEquals(3, g.getEdgesCount());
    }

    @Test
    public void stringToGraphModel_emptyGraph() {
        GraphModel g = G6Format.stringToGraphModel("B?");
        assertEquals(3, g.numOfVertices());
        assertEquals(0, g.getEdgesCount());
    }

    @Test
    public void stringToGraphModel_K2() {
        GraphModel g = G6Format.stringToGraphModel("A_");
        assertEquals(2, g.numOfVertices());
        assertEquals(1, g.getEdgesCount());
    }

    // --- graphToG6 roundtrip ---

    @Test
    public void graphToG6_K3_roundtrip() {
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        String encoded = G6Format.graphToG6(g);
        GraphModel decoded = G6Format.stringToGraphModel(encoded);
        assertEquals(3, decoded.numOfVertices());
        assertEquals(3, decoded.getEdgesCount());
    }

    @Test
    public void graphToG6_singleEdge_roundtrip() {
        GraphModel g = buildGraph(2, new int[][]{{0,1}});
        String encoded = G6Format.graphToG6(g);
        GraphModel decoded = G6Format.stringToGraphModel(encoded);
        assertEquals(2, decoded.numOfVertices());
        assertEquals(1, decoded.getEdgesCount());
    }

    @Test
    public void graphToG6_emptyGraph_roundtrip() {
        GraphModel g = buildGraph(4, new int[][]{});
        String encoded = G6Format.graphToG6(g);
        GraphModel decoded = G6Format.stringToGraphModel(encoded);
        assertEquals(4, decoded.numOfVertices());
        assertEquals(0, decoded.getEdgesCount());
    }

    // --- createAdjMatrix ---

    @Test
    public void createAdjMatrix_K2_singleOne() {
        // K2 adjacency matrix: [[0,1],[1,0]]
        // lower triangle scan: j=1,i=0 → entry (0,1) = 1 → "1"
        GraphModel g = buildGraph(2, new int[][]{{0,1}});
        Jama.Matrix m = g.getAdjacencyMatrix();
        String bits = G6Format.createAdjMatrix(m);
        assertEquals("1", bits);
    }

    @Test
    public void createAdjMatrix_K3_allOnes() {
        GraphModel g = buildGraph(3, new int[][]{{0,1},{0,2},{1,2}});
        Jama.Matrix m = g.getAdjacencyMatrix();
        String bits = G6Format.createAdjMatrix(m);
        assertEquals("111", bits);
    }

    @Test
    public void createAdjMatrix_emptyThreeVertex_allZeros() {
        GraphModel g = buildGraph(3, new int[][]{});
        Jama.Matrix m = g.getAdjacencyMatrix();
        String bits = G6Format.createAdjMatrix(m);
        assertEquals("000", bits);
    }

    // --- SIZELEN ---

    @Test
    public void sizelen_small() {
        assertEquals(1, G6Format.SIZELEN(0));
        assertEquals(1, G6Format.SIZELEN(62));
    }

    @Test
    public void sizelen_medium() {
        assertEquals(4, G6Format.SIZELEN(63));
        assertEquals(4, G6Format.SIZELEN(258047));
    }

    @Test
    public void sizelen_large() {
        assertEquals(8, G6Format.SIZELEN(258048));
    }
}
