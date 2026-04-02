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

## License

GSearchTea is open-source and released under the [GPL License](LICENSE).
