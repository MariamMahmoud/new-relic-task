package com.newrelic.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.text.html.parser.Entity;

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
            String metricName = matcher.group(2);
            var value = Integer.parseInt(matcher.group(3));

            // construct input: { metricName: [{instant0,value0}, {instant1,value1}] }
            var metricValList = input.computeIfAbsent(metricName, k ->  new LinkedList<>());
            var instVal = Map.entry(instant, value);
            metricValList.add(instVal);
        }

        input.entrySet()
        .parallelStream()
        .forEach(entry -> { // make this parallel for each input i
            // { i: [{instant0,value0}, {instant1,value1}] }
            // create values tree for each metricInput
            Map<Instant, List<Integer>> consumedMetric = consumeOneMetric(entry.getValue());
            var averagedMetric = averageMetric(consumedMetric);
            output.put(entry.getKey(), averagedMetric);
        });

        return output;
    }

    private Map<Instant, List<Integer>> consumeOneMetric(List<Entry<Instant, Integer>> metric) {
        Map<Instant, List<Integer>> metricValues = new TreeMap<Instant, List<Integer>>(); 

        metric.stream()
        .forEach(val -> {
            var instant = val.getKey(); // instant
            var value = val.getValue(); // int value
            // fill up metricValues
            var values = metricValues.computeIfAbsent(instant.truncatedTo(ChronoUnit.MINUTES), k -> new LinkedList<>());
            values.add(value);
        });

        return metricValues;
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
    // new Comparator<Instant>() {
    //     @Override
    //     public int compare(Instant a, Instant b)
    //     {
    //         return b.compareTo(a);
    //     }
    // }
}
