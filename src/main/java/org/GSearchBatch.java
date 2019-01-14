package org;

import graphtea.extensions.G6Format;
import graphtea.graph.graph.GraphModel;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

public class GSearchBatch {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataSet<String> text = env.readTextFile("all7.g6");
        text.map(new MapFunction<String, GraphModel>() {
            @Override
            public GraphModel map(String s) throws Exception {
                return G6Format.stringToGraphModel(s);
            }
        }).print();
    }
}
