/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org;

import graphtea.extensions.G6Format;
import graphtea.extensions.reports.zagreb.ZagrebIndex;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.util.Collections;

/**
 * Implements the "WordCount" program that computes a simple word occurrence histogram
 * over text files.
 *
 * <p>
 * The input is a plain text file with lines separated by newline characters.
 * <p>
 * This example shows how to:
 * <ul>
 * <li>write a simple Flink program.
 * <li>use Tuple data types.
 * <li>write and use user-defined functions.
 * </ul>
 *
 */
@SuppressWarnings("serial")
public class WordCount {

    // *************************************************************************
    //     PROGRAM
    // *************************************************************************

    public static void main(String[] args) throws Exception {
//        final ParameterTool params = ParameterTool.fromArgs(args);
        // set up the execution environment
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // make parameters available in the web interface
//        env.getConfig().setGlobalJobParameters(params);

        // get input data
        //DataSet<String> text = env.readTextFile("file:///home/rostam/kara/test.tt");
        DataSet<String> text = env.readTextFile("file:///home/rostam/kara/graphs/all8.g6");

        //GraphModel g = new GraphModel(false);
        DataSet<Tuple2<String, Double>> counts =
                // split up the lines in pairs (2-tuples) containing: (word,1)
                text.flatMap(new Tokenizer())
                        .groupBy(1)
                        .reduce(new StringSum()).
                        flatMap((FlatMapFunction<Tuple2<String, Double>, Tuple2<String, Double>>)
                                (stringDoubleTuple2, collector) ->
                                        collector.collect(new Tuple2<String,Double>(""
                                                +(count(stringDoubleTuple2.getField(0)
                                ,',')+1),stringDoubleTuple2.getField(1))));

//        // emit result
//        if (params.has("output")) {
           // counts.writeAsCsv("file:///home/rostam/kara/graphs/output.txt", "\n", " ");
//            // execute program
//            env.execute("WordCount Example");
//        } else {
//            System.out.println("Printing result to stdout. Use --output to specify output path.");
           counts.print();
//        }
    }

    // *************************************************************************
    //     USER FUNCTIONS
    // *************************************************************************

    public static int count(String s, char c) {
        int cnt=0;
        for(int i=0;i<s.length();i++)
            if(s.charAt(i) == c) cnt++;
        return cnt;
    }

    public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Double>> {
        public void flatMap(String value, Collector<Tuple2<String, Double>> out) {
//            out.collect(new Tuple2<String, Double>(value,
//                    Double.parseDouble(new ZagrebIndex().calculate(G6Format.stringToGraphModel(value)).toString())));

            out.collect(new Tuple2<>(value,
                    (double)G6Format.stringToGraphModel(value).getEdgesCount()));
        }
    }

    public static class StringSum implements ReduceFunction<Tuple2<String, Double>> {
        @Override
        public Tuple2<String,Double> reduce(Tuple2<String, Double> in1, Tuple2<String, Double> in2) {
            return new Tuple2<String,Double>(in1.getField(0)+","+in2.getField(0), in2.getField(1));
        }
    }
}

