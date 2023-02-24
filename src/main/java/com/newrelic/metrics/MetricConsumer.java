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
    private Map<String, List<Entry<Instant, Integer>>> input = new TreeMap<>();
    private Map<String, Map<Instant, Double>> output = new TreeMap<>();

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

        input
        .entrySet()
        .parallelStream()
        .forEach(entry -> {
            var k = entry.getKey();
            var v = entry.getValue();
            Map<Instant, List<Integer>> metricValues = new TreeMap<>();
            var aggregatedMetric = aggregatePerMetric(v, metricValues);
            var average = averageMetric(aggregatedMetric);
            output.put(k, new TreeMap<>(average));
        });

        return output;
    }

    private Map<Instant, List<Integer>> aggregatePerMetric(List<Entry<Instant, Integer>> v, Map<Instant, List<Integer>>metricValues) {
        v.stream().forEach(val -> aggregateMinuites(val.getKey(), val.getValue(), metricValues));
        return metricValues;
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
