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
| `stream [file]` | Run `StreamingReport` — computes ABC index per graph (default: `test.g6`) |
| `spectral` | Run `Test` — spectral property search via `nauty-geng` (no Flink) |
| `all [file]` | Build then run `stream` |

```bash
./run.sh stream my_graphs.g6   # use a custom G6 file
./run.sh spectral              # requires nauty-geng on PATH
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
| `org.StreamingReport` | `test.g6` | Flink streaming: computes ABC index per graph in 5s time windows |
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
  org/                          # Entry points (GSearch, GSearchBatch, StreamingReport, Test)
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

## License

GSearchTea is open-source and released under the [GPL License](LICENSE).
