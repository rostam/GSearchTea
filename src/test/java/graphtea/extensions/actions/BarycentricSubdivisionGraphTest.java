package graphtea.extensions.actions;

import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GPoint;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;
import org.junit.Test;

import static org.junit.Assert.*;

public class BarycentricSubdivisionGraphTest {

    private GraphModel buildPath(int n) {
        GraphModel g = new GraphModel(false);
        Vertex[] v = new Vertex[n];
        for (int i = 0; i < n; i++) {
            v[i] = new Vertex();
            // Give each vertex a distinct location so subdivision points are calculable
            v[i].setLocation(new GPoint(i * 100, 0));
            g.addVertex(v[i]);
        }
        for (int i = 0; i < n - 1; i++) {
            g.addEdge(new Edge(v[i], v[i + 1]));
        }
        return g;
    }

    private GraphModel buildK3() {
        GraphModel g = new GraphModel(false);
        Vertex[] v = new Vertex[3];
        v[0] = new Vertex(); v[0].setLocation(new GPoint(0, 0));
        v[1] = new Vertex(); v[1].setLocation(new GPoint(100, 0));
        v[2] = new Vertex(); v[2].setLocation(new GPoint(50, 100));
        for (Vertex vertex : v) g.addVertex(vertex);
        g.addEdge(new Edge(v[0], v[1]));
        g.addEdge(new Edge(v[0], v[2]));
        g.addEdge(new Edge(v[1], v[2]));
        return g;
    }

    // k=1: each original edge is replaced by a path of 2 edges (one new vertex)
    @Test
    public void k1_singleEdge_vertexCount() {
        // 2 original vertices + 1 subdivision point = 3
        GraphModel g = buildPath(2);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 1);
        assertEquals(3, result.numOfVertices());
    }

    @Test
    public void k1_singleEdge_edgeCount() {
        // 1 original edge becomes 2 edges
        GraphModel g = buildPath(2);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 1);
        assertEquals(2, result.getEdgesCount());
    }

    // k=2: each edge gets 2 new subdivision vertices → 3 new edges per original edge
    @Test
    public void k2_singleEdge_vertexCount() {
        // 2 original + 2 subdivision = 4
        GraphModel g = buildPath(2);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 2);
        assertEquals(4, result.numOfVertices());
    }

    @Test
    public void k2_singleEdge_edgeCount() {
        GraphModel g = buildPath(2);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 2);
        assertEquals(3, result.getEdgesCount());
    }

    // Path P3 (2 edges) with k=1: 3 original + 2 new = 5 vertices, 4 edges
    @Test
    public void k1_path3_vertexCount() {
        GraphModel g = buildPath(3);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 1);
        assertEquals(5, result.numOfVertices());
    }

    @Test
    public void k1_path3_edgeCount() {
        GraphModel g = buildPath(3);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 1);
        assertEquals(4, result.getEdgesCount());
    }

    // K3 (3 edges) with k=1: 3 + 3 = 6 vertices, 6 edges
    @Test
    public void k1_K3_vertexAndEdgeCount() {
        GraphModel g = buildK3();
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 1);
        assertEquals(6, result.numOfVertices());
        assertEquals(6, result.getEdgesCount());
    }

    // General formula: |V(G')| = |V(G)| + k*|E(G)|,  |E(G')| = (k+1)*|E(G)|
    @Test
    public void generalFormula_k3_K3() {
        GraphModel g = buildK3();
        int k = 3;
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, k);
        assertEquals(3 + k * 3, result.numOfVertices());
        assertEquals((k + 1) * 3, result.getEdgesCount());
    }

    // k=0: graph should be isomorphic to original (no subdivision)
    @Test
    public void k0_identicalToOriginal() {
        GraphModel g = buildPath(3);
        GraphModel result = BarycentricSubdivisionGraph.createBarycentricGraph(g, 0);
        assertEquals(g.numOfVertices(), result.numOfVertices());
        assertEquals(g.getEdgesCount(), result.getEdgesCount());
    }
}
