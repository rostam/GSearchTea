package org;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import graphtea.extensions.G6Format;
import graphtea.graph.graph.GraphModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Test {
    static double round(double value, int decimalPlace) {
        double power_of_ten = 1;
        while (decimalPlace-- > 0) power_of_ten *= 10.0;
        return Math.round(value * power_of_ten) / power_of_ten;
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        int n = 10;
        Process process = Runtime.getRuntime().exec("nauty-geng -c " + n);
        InputStream is = process.getInputStream();
        Scanner sc = new Scanner(is);
        int count = 0;
        while (sc.hasNext()) {
            GraphModel g = G6Format.stringToGraphModel(sc.next());
            Matrix A = g.getWeightedAdjacencyMatrix();
            EigenvalueDecomposition ed = A.eig();
            double rv[] = ed.getRealEigenvalues();
            double iv[] = ed.getImagEigenvalues();
            double sum = 0;
            double sum_i = 0;
            for (int i = 0; i < rv.length; i++)
                sum += Math.abs(rv[i]);
            for (int i = 0; i < iv.length; i++)
                sum_i += Math.abs(iv[i]);
            sum = round(sum, 5);
            if (sum == (2 * n - 2)) count++;
        }
        count -= 1;
        System.out.printf(count +  "\n");
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println(estimatedTime - startTime);
    }
}
