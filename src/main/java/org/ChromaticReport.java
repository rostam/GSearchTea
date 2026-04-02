package org;

import graphtea.extensions.G6Format;
import graphtea.extensions.reports.ChromaticNumber;
import graphtea.graph.graph.GraphModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads a G6 file, computes the chromatic number for every graph,
 * and prints a frequency table to stdout.
 *
 * Usage: java org.ChromaticReport [g6file]   (default: all7.g6)
 */
public class ChromaticReport {
    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : "all7.g6";
        List<String> lines = Files.readAllLines(Paths.get(path));

        Map<Integer, Integer> freq = new TreeMap<>();
        int total = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            GraphModel g = G6Format.stringToGraphModel(line);
            int chi = ChromaticNumber.getChromaticNumber(g);
            freq.merge(chi, 1, Integer::sum);
            total++;
        }

        System.out.println("File   : " + path);
        System.out.println("Graphs : " + total);
        System.out.println();
        System.out.printf("%-16s  %-8s  %s%n", "Chromatic number", "Count", "Percent");
        System.out.println("------------------------------------------");
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            double pct = 100.0 * e.getValue() / total;
            System.out.printf("%-16d  %-8d  %.1f%%%n", e.getKey(), e.getValue(), pct);
        }
    }
}
