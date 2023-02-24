package com.newrelic.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetricConsumer {

    private Pattern pattern = Pattern.compile("(\\d+) (\\w+) (\\d+)");
    private Map<Instant, List<Integer>> cpuValues = new TreeMap<>();
    private Map<Instant, List<Integer>> memValues = new TreeMap<>();
    private Map<String, List<Entry<Instant, Integer>>> input = new TreeMap<>();

    public Map<String, Map<Instant, Double>> consume(InputStream is) throws IOException {

        var reader = new BufferedReader(new InputStreamReader(is));
        var line = "";
        while ((line = reader.readLine()) != null) {
            var matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            var instant = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
            var metricName = matcher.group(2);
            var metricValue = Integer.parseInt(matcher.group(3));
            Entry<Instant,Integer> inputEntry = Map.entry(instant, metricValue);
            var itemsList = input.computeIfAbsent(metricName, k -> new LinkedList<>());
            itemsList.add(inputEntry);
        }

        // calculate outside loop            
        input
        .entrySet()
        .parallelStream()
        .forEach(entry -> {
            var k = entry.getKey();
            var v = entry.getValue();
            if (k.equals("cpu")) {
                v.stream().forEach(val -> aggregateMinuites(val.getKey(), val.getValue(), cpuValues));
            } else if (k.equals("mem")) {
                v.stream().forEach(val -> aggregateMinuites(val.getKey(), val.getValue(), memValues));
            }
        });

        var cpuAverages =  averageMetric(cpuValues);
        var memAverages = averageMetric(memValues);

        return Map.of(
                "cpu", new TreeMap<>(cpuAverages),
                "mem", new TreeMap<>(memAverages));
    }

    private void aggregateMinuites(Instant instant, int metricValue, Map<Instant, List<Integer>> metricValues) {
        var valuesCpu = metricValues.computeIfAbsent(instant.truncatedTo(ChronoUnit.MINUTES), k -> new LinkedList<>());
        valuesCpu.add(metricValue);
    }

    private Map<Instant, Double> averageMetric(Map<Instant, List<Integer>> values) {
        return values.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToInt(i -> i)
                                .average()
                                .orElse(-1)
                ));
    }
}
