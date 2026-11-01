
package com.example.sovereign_ai.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ChartGenerationService {

    private static final Set<String> SUPPORTED_CHARTS =
            Set.of("bar", "line", "pie", "scatter");

    private static final Set<String> SUPPORTED_AGGREGATIONS =
            Set.of("none", "sum", "average", "count");

    private static final int MAX_CHART_POINTS = 1000;

    private final UniversalDataAnalysisService analysis;

    public ChartGenerationService(
            UniversalDataAnalysisService analysis
    ) {
        this.analysis = analysis;
    }

    public Map<String, Object> generate(
            Map<String, Object> dataset,
            String xColumn,
            String yColumn,
            String chartType,
            String aggregation
    ) {

        // ============================================
        // 1. VALIDATE INPUT
        // ============================================

        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dataset cannot be empty."
            );
        }

        if (xColumn == null || xColumn.isBlank()
                || yColumn == null || yColumn.isBlank()) {
            throw new IllegalArgumentException(
                    "X and Y columns must be specified."
            );
        }

        xColumn = xColumn.trim();
        yColumn = yColumn.trim();

        if (xColumn.equals(yColumn)) {
            throw new IllegalArgumentException(
                    "X and Y columns must be different."
            );
        }

        String type = chartType == null || chartType.isBlank()
                ? "bar"
                : chartType.trim().toLowerCase(Locale.ROOT);

        String mode = aggregation == null || aggregation.isBlank()
                ? "none"
                : aggregation.trim().toLowerCase(Locale.ROOT);

        if (!SUPPORTED_CHARTS.contains(type)) {
            throw new IllegalArgumentException(
                    "Unsupported chart type: " + type
            );
        }

        if (!SUPPORTED_AGGREGATIONS.contains(mode)) {
            throw new IllegalArgumentException(
                    "Unsupported aggregation: " + mode
            );
        }

        if ("scatter".equals(type) && !"none".equals(mode)) {
            throw new IllegalArgumentException(
                    "Scatter charts do not support aggregation."
            );
        }

        // ============================================
        // 2. EXTRACT DATASET ROWS AND COLUMNS
        // ============================================

        List<Map<String, Object>> rows =
                analysis.getRows(dataset);

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Dataset contains no rows."
            );
        }

        List<String> columns =
                analysis.getColumns(dataset, rows);

        if (columns == null
                || !columns.contains(xColumn)
                || !columns.contains(yColumn)) {
            throw new IllegalArgumentException(
                    "Selected columns do not exist in dataset."
            );
        }

        List<Map<String, Object>> chartData =
                new ArrayList<>();

        // ============================================
        // 3. NON-AGGREGATED DATA
        // ============================================

        if ("none".equals(mode)) {

            for (Map<String, Object> row : rows) {

                if (row == null) {
                    continue;
                }

                Object x = row.get(xColumn);

                if (x == null || x.toString().isBlank()) {
                    continue;
                }

                Double y = analysis.parseNumber(
                        row.get(yColumn)
                );

                if (y == null || !Double.isFinite(y)) {
                    continue;
                }

                if ("scatter".equals(type)) {

                    Double numericX =
                            analysis.parseNumber(x);

                    if (numericX == null
                            || !Double.isFinite(numericX)) {
                        continue;
                    }

                    x = numericX;
                }

                if ("pie".equals(type) && y < 0) {
                    continue;
                }

                Map<String, Object> point =
                        new LinkedHashMap<>();

                point.put(xColumn, x);
                point.put(yColumn, y);

                chartData.add(point);

                if (chartData.size() > MAX_CHART_POINTS) {
                    throw new IllegalArgumentException(
                            "Chart exceeds maximum of "
                                    + MAX_CHART_POINTS + " points."
                    );
                }
            }

        } else {

            // ========================================
            // 4. AGGREGATED DATA
            // ========================================

            Map<Object, AggregateState> groups =
                    new LinkedHashMap<>();

            for (Map<String, Object> row : rows) {

                if (row == null) {
                    continue;
                }

                Object category = row.get(xColumn);

                if (category == null
                        || category.toString().isBlank()) {
                    continue;
                }

                AggregateState state =
                        groups.computeIfAbsent(
                                category,
                                key -> new AggregateState()
                        );

                if ("count".equals(mode)) {
                    state.count++;
                    continue;
                }

                Double value = analysis.parseNumber(
                        row.get(yColumn)
                );

                if (value == null || !Double.isFinite(value)) {
                    continue;
                }

                state.sum += value;
                state.count++;
            }

            for (Map.Entry<Object, AggregateState> entry
                    : groups.entrySet()) {

                AggregateState state = entry.getValue();

                if (state.count == 0) {
                    continue;
                }

                double result = switch (mode) {
                    case "sum" -> state.sum;
                    case "average" ->
                            state.sum / state.count;
                    case "count" -> state.count;
                    default -> throw new IllegalStateException(
                            "Unexpected aggregation: " + mode
                    );
                };

                if (!Double.isFinite(result)) {
                    continue;
                }

                if ("pie".equals(type) && result < 0) {
                    continue;
                }

                Map<String, Object> point =
                        new LinkedHashMap<>();

                point.put(xColumn, entry.getKey());
                point.put(yColumn, result);

                chartData.add(point);

                if (chartData.size() > MAX_CHART_POINTS) {
                    throw new IllegalArgumentException(
                            "Chart exceeds maximum of "
                                    + MAX_CHART_POINTS + " points."
                    );
                }
            }
        }

        // ============================================
        // 5. VALIDATE GENERATED CHART
        // ============================================

        if (chartData.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid numerical data available "
                            + "for chart generation."
            );
        }

        // ============================================
        // 6. BUILD REACT-COMPATIBLE RESPONSE
        // ============================================

        String title = yColumn + " by " + xColumn;

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("type", "chart");
        response.put("chartType", type);
        response.put("title", title);
        response.put("xKey", xColumn);
        response.put("yKey", yColumn);
        response.put("aggregation", mode);
        response.put("data", chartData);
        response.put("dataPoints", chartData.size());

        return response;
    }

    // ================================================
    // INTERNAL AGGREGATION STATE
    // ================================================

    private static class AggregateState {
        private double sum = 0.0;
        private int count = 0;
    }
}
