#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR="target/gteaflink-1.0-SNAPSHOT.jar"

# Flink 1.6 uses unsafe reflection that the Java 9+ module system blocks.
# These flags restore the access Flink's Kryo serializer needs at runtime.
JVM_FLAGS="--add-opens java.base/java.nio=ALL-UNNAMED
           --add-opens java.base/sun.nio.ch=ALL-UNNAMED
           --add-opens java.base/java.lang=ALL-UNNAMED
           --add-opens java.base/java.net=ALL-UNNAMED
           --add-opens java.base/java.util=ALL-UNNAMED
           --add-opens java.base/java.io=ALL-UNNAMED"

usage() {
    echo "Usage: $0 <command> [g6file]"
    echo ""
    echo "Commands:"
    echo "  build       Build the project (skips tests)"
    echo "  test        Run all unit tests"
    echo "  stream      Run StreamingReport  - ABC index per graph     (reads: test.g6)"
    echo "  spectral    Run Test             - spectral property search (requires: nauty)"
    echo "  all         Build then run stream"
    echo ""
    echo "Note: Flink 1.6 was designed for Java 8. Some features may not work on Java 11+."
    echo "  'stream'  : works but produces no output when window expires before stream ends"
    echo "  'batch'   : GraphModel is not Flink-serializable; not supported"
    echo "  'spectral': does not use Flink; works on any Java version"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 test"
    echo "  $0 stream"
    echo "  $0 stream my_graphs.g6"
    echo "  $0 spectral"
    echo "  $0 all"
}

build() {
    echo "==> Building..."
    mvn package -DskipTests
    echo "==> Build complete: $JAR"
}

require_jar() {
    if [ ! -f "$JAR" ]; then
        echo "==> Jar not found. Building first..."
        build
    fi
}

# Returns classpath: project jar + all dependency jars in target/
classpath() {
    echo "$JAR:$(find target -maxdepth 1 -name '*.jar' ! -name "$(basename "$JAR")" | tr '\n' ':')"
}

CMD="${1:-}"

case "$CMD" in
    build)
        build
        ;;
    test)
        echo "==> Running tests..."
        mvn test
        ;;
    stream)
        require_jar
        # StreamingReport reads test.g6; copy custom file there if provided
        if [ -n "${2:-}" ] && [ "${2}" != "test.g6" ]; then
            cp -f "$2" test.g6
            echo "==> Using input file: $2"
        fi
        if [ ! -f "test.g6" ]; then
            echo "Error: test.g6 not found" >&2
            exit 1
        fi
        echo "==> Running StreamingReport (Ctrl-C to stop)..."
        java $JVM_FLAGS -cp "$(classpath)" org.StreamingReport
        ;;
    spectral)
        require_jar
        if ! command -v nauty-geng &>/dev/null; then
            echo "Error: nauty-geng not found. Install with: sudo apt install nauty" >&2
            exit 1
        fi
        echo "==> Running spectral property search..."
        java -cp "$(classpath)" org.Test
        ;;
    all)
        build
        if [ -n "${2:-}" ] && [ "${2}" != "test.g6" ]; then
            cp -f "$2" test.g6
            echo "==> Using input file: $2"
        fi
        echo "==> Running StreamingReport (Ctrl-C to stop)..."
        java $JVM_FLAGS -cp "$(classpath)" org.StreamingReport
        ;;
    ""|help|--help|-h)
        usage
        ;;
    *)
        echo "Error: Unknown command: $CMD" >&2
        usage
        exit 1
        ;;
esac
