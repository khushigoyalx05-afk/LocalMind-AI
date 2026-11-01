
package com.example.sovereign_ai.Service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UniversalDataAnalysisService {

    public Map<String, Object> analyze(
            Map<String, Object> dataset
    ) {

        List<Map<String, Object>> rows = getRows(dataset);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowCount", rows.size());

        Map<String, Object> statistics = new LinkedHashMap<>();

        for (String column : getColumns(dataset, rows)) {

            List<Double> values = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                Double number = parseNumber(row.get(column));

                if (number != null) {
                    values.add(number);
                }
            }

            if (values.isEmpty()) {
                continue;
            }

            double sum = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            double min = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0);

            double max = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0);

            Map<String, Object> columnStats =
                    new LinkedHashMap<>();

            columnStats.put("count", values.size());
            columnStats.put("sum", sum);
            columnStats.put("average", sum / values.size());
            columnStats.put("minimum", min);
            columnStats.put("maximum", max);

            statistics.put(column, columnStats);
        }

        result.put("statistics", statistics);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRows(
            Map<String, Object> dataset
    ) {
        Object rows = dataset.get("rows");

        if (!(rows instanceof List<?>)) {
            throw new IllegalArgumentException(
                    "Dataset must contain a rows array."
            );
        }

        return (List<Map<String, Object>>) rows;
    }

    @SuppressWarnings("unchecked")
    public List<String> getColumns(
            Map<String, Object> dataset,
            List<Map<String, Object>> rows
    ) {

        Object columns = dataset.get("columns");

        if (columns instanceof List<?>) {
            return (List<String>) columns;
        }

        Set<String> inferred = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            inferred.addAll(row.keySet());
        }

        return new ArrayList<>(inferred);
    }

    public Double parseNumber(Object value) {

        if (value == null || value instanceof Boolean) {
            return null;
        }

        if (value instanceof Number number) {
            double result = number.doubleValue();
            return Double.isFinite(result) ? result : null;
        }

        String text = value.toString()
                .trim()
                .replace(",", "");

        if (text.isEmpty()) {
            return null;
        }

        try {
            double result = Double.parseDouble(text);
            return Double.isFinite(result) ? result : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
