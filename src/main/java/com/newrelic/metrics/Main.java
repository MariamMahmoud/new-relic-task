package com.newrelic.metrics;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class Main {
    public static final String inputFile = "input.txt";
    public static final String outputFile = "output.txt";

    public static void main(String[] args) throws IOException {

        var consumer = new MetricConsumer();

        var start = Instant.now(); // maybe use clock instead of instant

        Map<String, Map<Instant, Double>> result;
        try(var is = new FileInputStream(inputFile)) {
            result = consumer.consume(is);
        }

        var end = Instant.now();

        var output = new StringBuilder();
        result.get("cpu").forEach((k, v) -> output.append(k).append(" cpu ").append(v).append("\n"));
        result.get("mem").forEach((k, v) -> output.append(k).append(" mem ").append(v).append("\n"));
        System.out.println(output);

        try(var os = new FileWriter(outputFile)) {
            os.write(output.toString());
        }

        System.out.println("END. process time: " + Duration.between(start, end));
    }
}
