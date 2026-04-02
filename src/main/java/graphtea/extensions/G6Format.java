package graphtea.extensions;

import Jama.Matrix;
import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import graphtea.graph.graph.Vertex;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by rostam on 2/11/17.
 * @author M. Ali Rostami
 */
public class G6Format {
    private static final int BIAS6 = 63;
    private static final int SMALLN = 62;
    private static final int SMALLISHN = 258047;
    private static final int TOPBIT6 = 32;
    private static final int WORDSIZE = 32;

    static int SIZELEN(int n) {
        return (n) <= SMALLN ? 1 : ((n) <= SMALLISHN ? 4 : 8);
    }

    static int SETWD(int pos) {
        return pos >> 5;
    }

    static int SETBT(int pos) {
        return pos & 037;
    }


    public static HashMap<Integer, Vector<Integer>> stringToGraph(String g6) {
        int n = graphsize(g6);
        HashMap<Integer, Vector<Integer>> graph = new HashMap<>();
        String p = g6;
        if (g6.charAt(0) == ':' || g6.charAt(0) == '&')
            p = g6.substring(1);
        p = p.substring(SIZELEN(n));

        int m = (n + WORDSIZE - 1) / WORDSIZE;
        int x=0;
        long[] g = new long[m * n];
        for (int ii = m * n; --ii > 0; ) g[ii] = 0;
        g[0] = 0;
        int k = 1;
        int it = 0;
        for (int j = 1; j < n; ++j) {
            for (int i = 0; i < j; ++i) {
                if (--k == 0) {
                    k = 6;
                    x = p.charAt(it) - BIAS6;
                    it++;
                }
                if ((x & TOPBIT6) != 0) {
                    if (graph.containsKey(i)) graph.get(i).add(j);
                    else {
                        graph.put(i, new Vector<>());
                        graph.get(i).add(j);
                    }
                }
                x <<= 1;
            }
        }
        return graph;
    }

    public static GraphModel stringToGraphModel(String g6) {
        int n = graphsize(g6);
        GraphModel graph = new GraphModel(false);
        for(int i=0;i<n;i++) {
            graph.addVertex(new Vertex());
        }
        String p = g6;
        if (g6.charAt(0) == ':' || g6.charAt(0) == '&')
            p = g6.substring(1);
        p = p.substring(SIZELEN(n));

        int m = (n + WORDSIZE - 1) / WORDSIZE;
        int x=0;
        long[] g = new long[m * n];
        for (int ii = m * n; --ii > 0; ) g[ii] = 0;
        g[0] = 0;
        int k = 1;
        int it = 0;
        for (int j = 1; j < n; ++j) {
            for (int i = 0; i < j; ++i) {
                if (--k == 0) {
                    k = 6;
                    x = p.charAt(it) - BIAS6;
                    it++;
                }
                if ((x & TOPBIT6) != 0)
                    graph.addEdge(new Edge(graph.getVertex(i),graph.getVertex(j)));
                x <<= 1;
            }
        }
        return graph;
    }

    /* Get size of graph out of graph6 or sparse6 string. */
    static int graphsize(String s) {
        String p;
        if (s.charAt(0) == ':') p = s.substring(1);
        else p = s;
        int n;
        n = p.charAt(0) - BIAS6;

        if (n > SMALLN) {
            n = p.charAt(1) - BIAS6;
            n = (n << 6) | (p.charAt(2) - BIAS6);
            n = (n << 6) | (p.charAt(3) - BIAS6);
        }
        return n;
    }

    ////////////////////////////////////////////////////////////////////
    ////////////// Generate G6 Format
    public static String graphToG6(GraphModel g) {
        return encodeGraph(g.numOfVertices(),createAdjMatrix(g.getAdjacencyMatrix()));
    }

    public static String createAdjMatrix(Matrix m) {
        StringBuilder result = new StringBuilder();
        for (int i = 1, k = 1; k < m.getColumnDimension(); i++, k++) {
            for (int j = 0; j < i; j++) {
                result.append(m.get(j, i) != 0 ? '1' : '0');
            }
        }
        return result.toString();
    }


    public static String encodeGraph(int NoNodes, String adjmatrix) {
        int[] nn = encodeN(NoNodes);
        int[] adj = encodeR(adjmatrix);
        int[] res = new int[nn.length + adj.length];
        System.arraycopy(nn, 0, res, 0, nn.length);
        System.arraycopy(adj, 0, res, nn.length, adj.length);
        StringBuilder rv = new StringBuilder();
        for (int re : res) {
            rv.append((char) re);
        }
        return rv.toString();
    }

    private static int[] encodeN(long i) {

        if (0 <= i && i <= 62) {
            return new int[] { (int) (i + 63) };
        } else if (63 <= i && i <= 258047) {
            int[] ret = new int[4];
            ret[0] = 126;
            int[] g = encodeChunks(padL(Long.toBinaryString(i), 18));
            System.arraycopy(g, 0, ret, 1, 3);
            return ret;
        } else {
            int[] ret = new int[8];
            ret[0] = 126;
            ret[1] = 126;
            int[] g = encodeChunks(padL(Long.toBinaryString(i), 36));
            System.arraycopy(g, 0, ret, 2, 6);
            return ret;
        }

    }

    /** Splits binary string into 6-bit chunks and encodes each as a G6 character (value + 63). */
    private static int[] encodeChunks(String a) {
        int[] bytes = new int[a.length() / 6];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = Integer.parseInt(a.substring(i * 6, i * 6 + 6), 2) + 63;
        }
        return bytes;
    }

    private static int[] encodeR(String a) {
        return encodeChunks(padR(a));
    }

    private static String padR(String str) {
        int padwith = 6 - (str.length() % 6);
        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < padwith; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static String padL(String str, int h) {
        StringBuilder retval = new StringBuilder();
        for (int i = 0; i < h - str.length(); i++) {
            retval.append('0');
        }
        return retval + str;
    }
}
