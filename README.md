# GSearchTea

GSearchTea searches graph space using Apache Flink for distributed batch and stream processing over [Graph6 (G6)](http://users.cecs.anu.edu.au/~bdm/data/formats.txt) encoded graph datasets.

## Overview

The project reads graphs from G6 files, converts them in parallel, computes graph-theoretic properties (spectral indices, ABC index, edge counts, etc.), and returns results. It is built on top of the [GraphTea](https://github.com/graphtheorysoftware/GraphTea) library for graph modelling and algorithms.

## Requirements

- Java 8+ (tested on Java 21 — see [compatibility note](#java-compatibility) below)
- Maven 3+
- `nauty-geng` for the spectral search program: `sudo apt install nauty`

## Quick Start

```bash
git clone https://github.com/rostam/GSearchTea
cd GSearchTea
./run.sh build
./run.sh test
./run.sh stream        # compute ABC index over test.g6
./run.sh spectral      # spectral property search (requires nauty)
```

## `run.sh` Commands

| Command | Description |
|---------|-------------|
| `build` | Compile the project, skip tests |
| `test` | Run all unit tests |
| `stream [file]` | Run `StreamingReport` — ABC index per graph (default: `test.g6`) |
| `chromatic [file]` | Run `ChromaticReport` — chromatic number frequency table (default: `all7.g6`) |
| `nonhomo` | Run `NonHomoReport` — nonhomo(G, H) for canonical graph pairs |
| `spectral` | Run `Test` — spectral property search via `nauty-geng` (no Flink) |
| `gui` | Launch `GraphReportGUI` — interactive Swing GUI |
| `all [file]` | Build then run `stream` |

```bash
./run.sh stream my_graphs.g6   # use a custom G6 file
./run.sh spectral              # requires nauty-geng on PATH
./run.sh gui                   # open the graphical interface
```

## Running Tests Only

```bash
mvn test                          # all tests
mvn test -Dtest=G6FormatTest      # single class
mvn test -Dtest=UtilsTest#choose  # single method
```

Test reports are written to `target/surefire-reports/`.

## Entry Points

| Class | Input | Description |
|-------|-------|-------------|
| `org.GraphReportGUI` | any G6 file (via file chooser) | Swing GUI: interactive report runner with table output and CSV export |
| `org.StreamingReport` | `test.g6` | Flink streaming: computes ABC index per graph |
| `org.ChromaticReport` | any G6 file | Chromatic number frequency table; no Flink dependency |
| `org.NonHomoReport`   | (none) | Computes nonhomo(G, H) for canonical graph pairs; no Flink dependency |
| `org.Test` | `nauty-geng` output | Spectral property search over generated graphs; no Flink dependency |
| `org.GSearch` | `all7.g6` | Flink batch: groups graphs by edge count (has a known type-erasure bug) |
| `org.GSearchBatch` | `all7.g6` | Flink batch: loads and prints all graphs (incompatible with Java 9+) |

## Input Data

Graph data is read from G6-format files — one graph per line. Two example files are included:

- `all7.g6` — all 853 connected graphs on 7 vertices
- `test.g6` — 106 graphs for streaming tests

To generate your own with [nauty](http://users.cecs.anu.edu.au/~bdm/nauty/):
```bash
nauty-geng -c 8 > all8.g6    # all connected 8-vertex graphs
```

## Java Compatibility

Flink 1.6 was designed for Java 8 and uses reflection techniques blocked by the Java 9+ module system. `run.sh` automatically applies the required `--add-opens` flags. Two entry points remain limited:

- **`GSearch`** — pre-existing Flink type-erasure bug in a lambda `FlatMapFunction`
- **`GSearchBatch`** — `GraphModel` holds a `java.awt.Font` field that Flink's Kryo serializer cannot handle on Java 9+

`StreamingReport` and `Test` (spectral) work on any Java version.

## Project Structure

```
src/main/java/
  org/                          # Entry points (GraphReportGUI, GSearch, GSearchBatch, StreamingReport, Test)
  graphtea/extensions/
    G6Format.java               # G6 encode/decode
    Utils.java                  # Graph utility methods (degree, Laplacian, etc.)
    actions/                    # Graph transformations (line graph, total graph, products, …)
    algorithms/                 # Classical algorithms (Dijkstra, Kruskal, Prim, …)
    generators/                 # Graph generators (complete, random, wheel, star, …)
    reports/                    # Graph property reports (chromatic number, bipartite, …)
    io/                         # Graph I/O (G6, MTX, JSON, simple text)
  graphtea/graph/               # GraphTea core: GraphModel, Vertex, Edge, GPoint
  graphtea/library/             # GraphTea base graph data structures
src/test/java/
  graphtea/extensions/
    G6FormatTest.java           # G6 encode/decode tests (18 tests)
    UtilsTest.java              # Utility method tests  (16 tests)
    actions/BarycentricSubdivisionGraphTest.java  (9 tests)
    generators/RandomGeneratorTest.java           (4 tests)
```

## GraphReportGUI

`GraphReportGUI` is an interactive Swing application for running any combination of graph reports over a G6 file without touching the command line.

```bash
./run.sh build   # first time only
./run.sh gui
```

### Features

- **Load any G6 file** via a file chooser dialog — graphs appear in a list on the left.
- **Select graphs** individually or with *Select All / Select None*.
- **Choose reports** from four categorised checkbox panels:

  | Category | Reports |
  |----------|---------|
  | General | Number of Vertices, Number of Edges, Max/Min Degree, Number of Triangles, Connected Components, Girth Size, Is Bipartite, Is Eulerian |
  | Coloring | Chromatic Number |
  | Topological Indices | Randic Index, Harmonic Index, Hyper Zagreb Index, Balaban Index |
  | Spectral | Laplacian Energy, Laplacian Energy-Like |

- **Run** — reports execute in a background thread with a progress bar; results appear row-by-row in a scrollable table.
- **Summary statistics** — min, max, mean, and count are computed automatically for every numeric column.
- **Export CSV** — saves the full results table to a `.csv` file.

### Screenshot layout

```
┌──────────────────────────────────────────────────────────────┐
│  [Open G6 File...]                                           │
│  ┌─── Graphs ─────┐  ┌─── Reports ─────────────────────────┐│
│  │ 1: I?AA@_gw?   │  │ ☑ Vertices  ☑ Edges  ☑ Chromatic…  ││
│  │ 2: I??E@_Ki?   │  └─────────────────────────────────────┘│
│  │ …              │  ┌─── Results ─────────────────────────┐│
│  │                │  │  Graph (G6) │ Vertices │ Chromatic… ││
│  │ [Select All]   │  │  I?AA@_gw?  │    10    │     2      ││
│  │ [Select None]  │  │  …          │   …      │   …        ││
│  └────────────────┘  └─────────────────────────────────────┘│
│                       ┌─── Summary ─────────────────────────┐│
│                       │  Chromatic Number  min=2 max=3 …    ││
│                       └─────────────────────────────────────┘│
│  [Run Reports]  [Export CSV...]          ████████░░  73%    │
└──────────────────────────────────────────────────────────────┘
```

## Results: ABC Index on Trees with 10 Vertices

The `StreamingReport` was run over `test.g6`, which contains all **106 non-isomorphic trees on 10 vertices**.
For each graph the [Atom-Bond Connectivity (ABC) index](https://doi.org/10.1021/ci990307l) was computed:

$$\text{ABC}(G) = \sum_{uv \in E} \sqrt{\frac{d_u + d_v - 2}{d_u \cdot d_v}}$$

where $d_u$, $d_v$ are the degrees of the endpoints of each edge.

### Summary Statistics

| Statistic | Value |
|-----------|-------|
| Graphs    | 106 trees, 10 vertices, 9 edges each |
| Min ABC   | 6.3235 |
| Max ABC   | 8.4853 |
| Mean ABC  | 6.9024 |
| Median ABC | 6.8705 |
| Distinct values | 74 out of 106 |

### Distribution

```
[6.5, 7.0)  ███████████████████████████████████████████  43 graphs  (40.6%)
[7.0, 7.5)  █████████████████████████████████████████████  45 graphs  (42.5%)
[7.5, 8.0)  ███████████████  15 graphs  (14.2%)
[8.0, 8.5)  ██   2 graphs   (1.9%)
[8.5, 9.0)  █    1 graph    (0.9%)
```

Most trees fall in the `[6.5, 7.5)` range (83%), with a long right tail toward the maximum.

### Extremes

**Maximum — `I??????~w` (ABC = 8.4853 = 6√2)**
The star graph K₁,₉: one hub vertex of degree 9 connected to 9 leaves of degree 1.
Every edge contributes √(8/9), giving ABC = 9 · √(8/9) = 6√2.
This is the unique maximiser of the ABC index among all trees on 10 vertices.

**Minimum — `I?AA@_gw?` (ABC = 6.3235)**
A caterpillar tree whose ABC index falls below the path P₁₀ (ABC = 9/√2 ≈ 6.3640).
Five trees in the dataset share the path value (9/√2), confirming that P₁₀ is not the
minimiser for n = 10 — consistent with known results in chemical graph theory showing
that specific caterpillar trees beat the path for n ≥ 10.

### Raw Output (sample)

```
(I?AA@_gw?,6.323521)   ← minimum (caterpillar tree)
(I??E@_Ki?,6.363961)   ← path P₁₀  (one of 5 trees at this value)
...
(I????A?~o,7.962114)
(I??????~w,8.485281)   ← maximum (star K₁,₉)
```

Full results can be reproduced with:
```bash
./run.sh stream
```

## Results: Chromatic Number on Connected Graphs with 7 Vertices

`ChromaticReport` was run over `all7.g6`, which contains all **853 connected graphs on 7 vertices**.
The chromatic number χ(G) is the minimum number of colours needed to colour the vertices so that no two adjacent vertices share the same colour.

### Frequency Table

| χ(G) | Count | Percent | Meaning |
|------|-------|---------|---------|
| 2    | 44    | 5.2%    | Bipartite (trees, even cycles, complete bipartite graphs) |
| 3    | 475   | 55.7%   | Non-bipartite, triangle-free or with odd cycles but no K₄ |
| 4    | 282   | 33.1%   | Contains K₄ or requires 4 colours |
| 5    | 46    | 5.4%    | Contains K₅ minor or dense subgraphs |
| 6    | 5     | 0.6%    | Near-complete graphs |
| 7    | 1     | 0.1%    | K₇ (the complete graph on 7 vertices) |

### Distribution

```
χ=2  ████                                                 44   (5.2%)
χ=3  ████████████████████████████████████████████████████ 475  (55.7%)
χ=4  ████████████████████████████████                    282  (33.1%)
χ=5  █████                                                46   (5.4%)
χ=6  █                                                     5   (0.6%)
χ=7  ▏                                                     1   (0.1%)
```

### Observations

- The majority of connected 7-vertex graphs (55.7%) require exactly 3 colours — reflecting how common odd cycles but not K₄ are in this graph class.
- Only 44 graphs (5.2%) are bipartite (χ = 2). All 106 trees in `test.g6` are bipartite (χ = 2 for all of them, confirmed separately).
- The unique χ = 7 graph is **K₇**, the complete graph where every pair of vertices is connected.
- The 5 graphs with χ = 6 are near-complete graphs missing only a small number of edges from K₇.

Reproduce with:
```bash
./run.sh chromatic            # runs on all7.g6
./run.sh chromatic test.g6    # runs on trees (all χ=2)
```

## Results: nonhomo — Non-Homomorphism Distance

### Definition

For graphs G and H, **nonhomo(G, H)** is the minimum number of edges that must be
removed from G so that the resulting graph admits a graph homomorphism to H.

A *graph homomorphism* f : G → H is a map from V(G) to V(H) that preserves
adjacency: whenever (u, v) is an edge in G, (f(u), f(v)) must be an edge in H.

Equivalently:

$$\text{nonhomo}(G, H) = \min_{f\,:\,V(G)\to V(H)} \bigl|\{(u,v)\in E(G) : (f(u),\,f(v))\notin E(H)\}\bigr|$$

**Special cases:**

| Expression | Meaning |
|---|---|
| nonhomo(G, K_k) | Minimum edges to remove to make G k-colourable |
| nonhomo(G, K_2) | Minimum edges to remove to make G bipartite (bipartite edge deletion number) |
| nonhomo(G, H) = 0 | G is already homomorphic to H |

**Implementation:** `NonHomomorphism.java` uses branch-and-bound over all
|V(H)|^|V(G)| vertex mappings, pruning a branch as soon as its accumulated
bad-edge count meets or exceeds the current best.

Run with:
```bash
./run.sh nonhomo
```

---

### nonhomo(K_n, K_m)

Complete graphs (rows are G = K_n, columns are H = K_m; entries marked — are not applicable since m > n):

| G \ H | K_1 | K_2 | K_3 | K_4 | K_5 | K_6 | K_7 | K_8 |
|-------|-----|-----|-----|-----|-----|-----|-----|-----|
| K_1   |   0 |  —  |  —  |  —  |  —  |  —  |  —  |  —  |
| K_2   |   1 |   0 |  —  |  —  |  —  |  —  |  —  |  —  |
| K_3   |   3 |   1 |   0 |  —  |  —  |  —  |  —  |  —  |
| K_4   |   6 |   2 |   1 |   0 |  —  |  —  |  —  |  —  |
| K_5   |  10 |   4 |   2 |   1 |   0 |  —  |  —  |  —  |
| K_6   |  15 |   6 |   3 |   2 |   1 |   0 |  —  |  —  |
| K_7   |  21 |   9 |   5 |   3 |   2 |   1 |   0 |  —  |
| K_8   |  28 |  12 |   7 |   4 |   3 |   2 |   1 |   0 |

**Closed form.** Since G → K_m iff G is m-colourable, the optimal strategy for
K_n → K_m is to partition the n vertices into m colour classes as evenly as
possible (sizes ⌊n/m⌋ and ⌈n/m⌉) and remove all within-class edges:

$$\text{nonhomo}(K_n, K_m) = r\binom{\lfloor n/m\rfloor+1}{2} + (m-r)\binom{\lfloor n/m\rfloor}{2},\quad r = n \bmod m$$

For example, K_7 → K_3: q=2, r=1 → 1·C(3,2) + 2·C(2,2) = 3 + 2 = **5** ✓

---

### nonhomo(C_n, K_2) — edges to remove to make a cycle bipartite

| n  | nonhomo(C_n, K_2) | note                    |
|----|-------------------|-------------------------|
|  3 |                 1 | odd cycle               |
|  4 |                 0 | even cycle (bipartite)  |
|  5 |                 1 | odd cycle               |
|  6 |                 0 | even cycle (bipartite)  |
|  7 |                 1 | odd cycle               |
|  8 |                 0 | even cycle (bipartite)  |
|  9 |                 1 | odd cycle               |
| 10 |                 0 | even cycle (bipartite)  |
| 11 |                 1 | odd cycle               |
| 12 |                 0 | even cycle (bipartite)  |
| 13 |                 1 | odd cycle               |
| 14 |                 0 | even cycle (bipartite)  |
| 15 |                 1 | odd cycle               |

**Pattern:** nonhomo(C_n, K_2) = 0 if n is even; 1 if n is odd.
Even cycles are bipartite by definition; removing any single edge from an odd
cycle produces a path (which is bipartite).

---

### nonhomo(C_n, C_5)

| n  | nonhomo(C_n, C_5) |
|----|-------------------|
|  3 |                 1 |
|  4 |                 0 |
|  5 |                 0 |
|  6 |                 0 |
|  7 |                 0 |
|  8 |                 0 |
|  9 |                 0 |
| 10 |                 0 |
| 11 |                 0 |
| 12 |                 0 |
| 13 |                 0 |
| 14 |                 0 |
| 15 |                 0 |

**Observations:**

- **C_3 → C_5 = 1:** C_3 = K_3 is a triangle; C_5 has girth 5 and contains no
  triangle, so no homomorphism exists. Removing one edge of C_3 yields a path
  P_3, which maps to C_5 easily. Thus 1 edge suffices.
- **C_n → C_5 = 0 for n ≥ 4:** A valid homomorphism exists for every n ≥ 4.
  For even n: any even cycle folds onto K_2 ⊂ C_5. For odd n ≥ 5: the
  wrapping map f(i) = i mod 5 works for all n (every "wrap edge" (n−1, 0)
  maps to an edge in C_5).
- The special case n = 3 is thus the only cycle requiring an edge removal to
  reach C_5.

---

### nonhomo(Petersen graph, K_k)

The Petersen graph GP(5,2): 10 vertices, 15 edges, 3-regular, girth 5,
chromatic number 3.

| k | nonhomo(Petersen, K_k) | interpretation                             |
|---|------------------------|--------------------------------------------|
| 1 |                     15 | K_1 has no edges; remove all 15 edges      |
| 2 |                      3 | Remove 3 edges to make Petersen bipartite  |
| 3 |                      0 | χ(Petersen) = 3 → already maps to K_3      |
| 4 |                      0 | χ ≤ k → already maps to K_4                |
| 5 |                      0 | trivially homomorphic to K_5               |

**Notable result:** nonhomo(Petersen, K_2) = **3**. Despite being 3-regular with
girth 5, the Petersen graph requires removing only 3 edges to become bipartite
(each of the five 5-cycles in the inner pentagram contributes to the obstruction;
the minimum odd-cycle edge-transversal has size 3).

---

### nonhomo(K_{m,n}, K_k) for small m, n

| G         | nonhomo(G, K_2) | nonhomo(G, K_3) |
|-----------|-----------------|-----------------|
| K_{1,1}   |               0 |               0 |
| K_{1,2}   |               0 |               0 |
| K_{1,3}   |               0 |               0 |
| K_{1,4}   |               0 |               0 |
| K_{2,2}   |               0 |               0 |
| K_{2,3}   |               0 |               0 |
| K_{2,4}   |               0 |               0 |
| K_{3,3}   |               0 |               0 |

All complete bipartite graphs satisfy nonhomo(K_{m,n}, K_k) = 0 for any k ≥ 2,
since they are bipartite (χ = 2) and every bipartite graph already admits a
homomorphism to K_2 ⊆ K_k.

---

## License

GSearchTea is open-source and released under the [GPL License](LICENSE).
