package org;

import graphtea.extensions.G6Format;
import graphtea.graph.graph.Edge;
import graphtea.graph.graph.GraphModel;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class StreamingReport {
    public static void main(String[] args) throws Exception {
      StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
      DataStream<Tuple2<String,Double>> dataStream =
        env.readTextFile("test.g6")
        .map(new ABCIndex());

      dataStream.print();

      env.execute("StreamingReport");
    }

    public static class ABCIndex implements MapFunction<String,Tuple2<String,Double>> {

      public Tuple2<String, Double> map(String s) {
        GraphModel g = G6Format.stringToGraphModel(s);
        double abc_index = 0;
        for(Edge e : g.getEdges()) {
          double d1 = g.getDegree(e.source);
          double d2 = g.getDegree(e.target);
          abc_index += Math.sqrt((d1+d2-2)/(d1*d2));
        }
        return new Tuple2<>(s, abc_index);
      }
    }
}

