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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetricConsumer {

    private Pattern pattern = Pattern.compile("(\\d+) (\\w+) (\\d+)");
    private Map<Instant, List<Integer>> cpuValues = new TreeMap<>();
    private Map<Instant, List<Integer>> memValues = new TreeMap<>();

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

            // DRY adding values
            if (metricName.equals("cpu")) {
                var valuesCpu = cpuValues.computeIfAbsent(instant.truncatedTo(ChronoUnit.MINUTES), k -> new LinkedList<>());
                valuesCpu.add(metricValue);
            } else if (metricName.equals("mem")) {
                var valuesMem = memValues.computeIfAbsent(instant.truncatedTo(ChronoUnit.MINUTES), k -> new LinkedList<>());
                valuesMem.add(metricValue);
            }
        }

        var cpuAverages =  averageMetric(cpuValues);
        var memAverages = averageMetric(memValues);

        return Map.of(
                "cpu", new TreeMap<>(cpuAverages),
                "mem", new TreeMap<>(memAverages));
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
