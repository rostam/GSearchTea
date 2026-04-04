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

Rows are G = K_n, columns are H = K_m (— means m > n, not applicable):

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

| n  | nonhomo(C_n, K_2) | note                   |
|----|-------------------|------------------------|
|  3 |                 1 | odd cycle              |
|  4 |                 0 | even cycle (bipartite) |
|  5 |                 1 | odd cycle              |
|  6 |                 0 | even cycle (bipartite) |
|  7 |                 1 | odd cycle              |
|  8 |                 0 | even cycle (bipartite) |
|  9 |                 1 | odd cycle              |
| 10 |                 0 | even cycle (bipartite) |
| 11 |                 1 | odd cycle              |
| 12 |                 0 | even cycle (bipartite) |
| 13 |                 1 | odd cycle              |
| 14 |                 0 | even cycle (bipartite) |
| 15 |                 1 | odd cycle              |

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
  P_3, which maps to C_5 easily.
- **C_n → C_5 = 0 for all n ≥ 4:** For even n, any even cycle folds onto
  K_2 ⊂ C_5. For odd n ≥ 5, the wrapping map f(i) = i mod 5 is a valid
  homomorphism (every wrap edge (n−1, 0) maps to a C_5 edge).
- C_3 is the only cycle requiring an edge removal to reach C_5.

---

### nonhomo(Petersen graph, K_k)

Petersen graph GP(5,2): 10 vertices, 15 edges, 3-regular, girth 5, χ = 3.

| k | nonhomo(Petersen, K_k) | interpretation                            |
|---|------------------------|-------------------------------------------|
| 1 |                     15 | K_1 has no edges; all edges must go       |
| 2 |                      3 | 3 edges suffice to make it bipartite      |
| 3 |                      0 | χ = 3 → already homomorphic to K_3        |
| 4 |                      0 | trivially homomorphic to K_4              |
| 5 |                      0 | trivially homomorphic to K_5              |

**Notable:** nonhomo(Petersen, K_2) = **3**. Despite being 3-regular with girth 5,
only 3 edge removals suffice to make it bipartite.

---

### Distributions over `all7.g6` — all 853 connected 7-vertex graphs

Computed for three targets: K_2, K_3, and C_5.
C_5 is a particularly informative target: it has no triangles (girth 5), so any
graph containing a triangle requires at least one edge removal to become
C_5-homomorphic.

#### nonhomo(G, K_2) — bipartite edge deletion number

| nonhomo | count | percent | distribution                     |
|---------|-------|---------|----------------------------------|
|       0 |    44 |   5.2%  | █████                            |
|       1 |   168 |  19.7%  | ████████████████████             |
|       2 |   267 |  31.3%  | ████████████████████████████████ |
|       3 |   219 |  25.7%  | ██████████████████████████       |
|       4 |   105 |  12.3%  | █████████████                    |
|       5 |    36 |   4.2%  | ████                             |
|       6 |    10 |   1.2%  | █                                |
|       7 |     2 |   0.2%  |                                  |
|       8 |     1 |   0.1%  |                                  |
|       9 |     1 |   0.1%  | ← K_7 (unique maximiser)         |

Only 44 of 853 graphs (5.2%) are bipartite. The mode is nonhomo = 2.
K_7 is the unique graph with nonhomo(K_7, K_2) = 9 — it requires removing the
most edges to become bipartite (balanced partition (4,3): C(4,2)+C(3,2)=9).

#### nonhomo(G, K_3) — edges to remove to make G 3-colourable

| nonhomo | count | percent | distribution                     |
|---------|-------|---------|----------------------------------|
|       0 |   519 |  60.8%  | ████████████████████████████████ |
|       1 |   267 |  31.3%  | ████████████████                 |
|       2 |    58 |   6.8%  | ████                             |
|       3 |     7 |   0.8%  |                                  |
|       4 |     1 |   0.1%  |                                  |
|       5 |     1 |   0.1%  | ← K_7                            |

60.8% of 7-vertex graphs are already 3-colourable (nonhomo = 0).
The chromatic numbers 4, 5, 6, 7 account for the 334 graphs with nonhomo ≥ 1.
K_7 requires 5 edge removals (balanced 3-partition of 7: 1·C(3,2)+2·C(2,2)=5).

#### nonhomo(G, C_5) — edges to remove to make G C_5-homomorphic

| nonhomo | count | percent | distribution                     |
|---------|-------|---------|----------------------------------|
|       0 |    59 |   6.9%  | ███████                          |
|       1 |   175 |  20.5%  | ██████████████████████           |
|       2 |   259 |  30.4%  | ████████████████████████████████ |
|       3 |   207 |  24.3%  | ██████████████████████████       |
|       4 |   103 |  12.1%  | █████████████                    |
|       5 |    36 |   4.2%  | ████                             |
|       6 |    10 |   1.2%  | █                                |
|       7 |     2 |   0.2%  |                                  |
|       8 |     1 |   0.1%  |                                  |
|       9 |     1 |   0.1%  | ← K_7                            |

Only 59 graphs (6.9%) are directly homomorphic to C_5 — 44 bipartite graphs plus
15 triangle-free 3-chromatic graphs whose circular chromatic number is ≤ 5/2.

---

### Cross-tabulation: nonhomo(G, K_3) × nonhomo(G, C_5)

Rows = nonhomo(G, K_3), columns = nonhomo(G, C_5), entries = number of graphs.

| K_3 \ C_5 |   0 |   1 |   2 |   3 |   4 |   5 |   6 |   7 |   8 |   9 | row sum |
|-----------|-----|-----|-----|-----|-----|-----|-----|-----|-----|-----|---------|
| **0**     |  59 | 175 | 202 |  74 |   9 |   — |   — |   — |   — |   — |     519 |
| **1**     |   — |   — |  57 | 133 |  67 |  10 |   — |   — |   — |   — |     267 |
| **2**     |   — |   — |   — |   — |  27 |  25 |   6 |   — |   — |   — |      58 |
| **3**     |   — |   — |   — |   — |   — |   1 |   4 |   2 |   — |   — |       7 |
| **4**     |   — |   — |   — |   — |   — |   — |   — |   — |   1 |   — |       1 |
| **5**     |   — |   — |   — |   — |   — |   — |   — |   — |   — |   1 |       1 |
| col sum   |  59 | 175 | 259 | 207 | 103 |  36 |  10 |   2 |   1 |   1 |     853 |

**Structural observations:**

1. **nonhomo(G, K_3) ≤ nonhomo(G, C_5)** holds for every graph (confirmed by all
   zeros below the diagonal). This follows because C_5 → K_3 (C_5 is 3-colourable),
   so any map from G to K_3 can be composed with a colouring of C_5 to give a map
   from G to K_3; the other direction: any valid map to K_3 removed at least as
   many "bad" edges as a map to C_5 must.

2. **The K_3-to-C_5 gap (row 0, columns 1–4): 460 graphs (54%)** are 3-colourable
   yet not homomorphic to C_5. These graphs contain a triangle — three mutually
   adjacent vertices that cannot all be mapped to distinct vertices of C_5 (which
   has no triangles). They require between 1 and 4 edge removals to become
   C_5-homomorphic.

3. **Perfect agreement on the diagonal below row 0:** Every graph with
   nonhomo(G, K_3) ≥ 1 has nonhomo(G, C_5) ≥ nonhomo(G, K_3) + 1 (the
   table never has a non-zero entry where C_5 < K_3).

---

### Notable examples from `all7.g6`

Top 10 by nonhomo(G, C_5), then by nonhomo(G, K_2):

| G6 string  | \|V\| | \|E\| | nonhomo(G, K_2) | nonhomo(G, K_3) | nonhomo(G, C_5) |
|------------|-------|-------|-----------------|-----------------|-----------------|
| `F~~~w`    |     7 |    21 |               9 |               5 |               9 |
| `F^~~w`    |     7 |    20 |               8 |               4 |               8 |
| `FV~~w`    |     7 |    19 |               7 |               3 |               7 |
| `F]~~w`    |     7 |    19 |               7 |               3 |               7 |
| `FF~~w`    |     7 |    18 |               6 |               2 |               6 |
| `FUZ~w`    |     7 |    16 |               6 |               2 |               6 |
| `FUz~w`    |     7 |    17 |               6 |               2 |               6 |
| `FU~~w`    |     7 |    18 |               6 |               3 |               6 |
| `FTz~w`    |     7 |    17 |               6 |               2 |               6 |
| `FTm~w`    |     7 |    16 |               6 |               3 |               6 |

`F~~~w` is K_7 (21 edges = C(7,2)); it maximises all three parameters simultaneously.

---

### Gap graphs: χ(G) ≤ 3 but nonhomo(G, C_5) > 0

3-colourable graphs that are not C_5-homomorphic (460 total; top 15 shown).
These are exactly the graphs with a triangle that are nonetheless 3-colourable —
equivalently, graphs whose circular chromatic number χ_c(G) > 5/2.

| G6 string  | \|V\| | \|E\| | nonhomo(G, K_2) | nonhomo(G, K_3) | nonhomo(G, C_5) |
|------------|-------|-------|-----------------|-----------------|-----------------|
| `FEr^o`    |     7 |    13 |               4 |               0 |               4 |
| `FEr~o`    |     7 |    14 |               4 |               0 |               4 |
| `FEj^o`    |     7 |    13 |               4 |               0 |               4 |
| `FEj~o`    |     7 |    14 |               4 |               0 |               4 |
| `FEvvW`    |     7 |    14 |               4 |               0 |               4 |
| `FEnvW`    |     7 |    14 |               4 |               0 |               4 |
| `FE~vW`    |     7 |    15 |               4 |               0 |               4 |
| `FFz~o`    |     7 |    16 |               4 |               0 |               4 |
| `FQzUW`    |     7 |    12 |               4 |               0 |               4 |
| `F?qjw`    |     7 |    10 |               3 |               0 |               3 |
| `F?qzw`    |     7 |    11 |               3 |               0 |               3 |
| `FCOfw`    |     7 |     9 |               3 |               0 |               3 |
| `FCQfw`    |     7 |    10 |               3 |               0 |               3 |
| `FCRfw`    |     7 |    11 |               3 |               0 |               3 |
| `FCQrW`    |     7 |     9 |               3 |               0 |               3 |

Reproduce all results with:
```bash
./run.sh nonhomo
```

---

## License

GSearchTea is open-source and released under the [GPL License](LICENSE).
