package graphtea.extensions.generators;

import graphtea.graph.graph.GraphModel;
import org.junit.Test;

import static org.junit.Assert.*;

public class RandomGeneratorTest {

    @Test
    public void generateRandomGraph_hasCorrectVertexCount() {
        GraphModel g = RandomGenerator.generateRandomGraph(10, 15);
        assertEquals(10, g.numOfVertices());
    }

    @Test
    public void generateRandomGraph_edgeCountAtMostRequested() {
        // GraphModel may deduplicate edges, so actual count ≤ requested
        GraphModel g = RandomGenerator.generateRandomGraph(10, 15);
        assertTrue(g.getEdgesCount() <= 15);
        assertTrue(g.getEdgesCount() > 0);
    }

    @Test
    public void generateRandomGraph_noStackOverflow_tinyGraph() {
        // Old recursive randomEdge() would StackOverflow on a 2-vertex graph
        // when many calls landed on the same vertex. Iterative version handles it.
        GraphModel g = RandomGenerator.generateRandomGraph(2, 20);
        assertEquals(2, g.numOfVertices());
        // With 2 vertices there is only 1 unique edge possible
        assertTrue(g.getEdgesCount() >= 1);
    }

    @Test
    public void generateRandomGraph_zeroEdges() {
        GraphModel g = RandomGenerator.generateRandomGraph(5, 0);
        assertEquals(5, g.numOfVertices());
        assertEquals(0, g.getEdgesCount());
    }
}
