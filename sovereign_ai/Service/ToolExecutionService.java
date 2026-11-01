
package com.example.sovereign_ai.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ToolExecutionService {

    private static final Logger log =
            LoggerFactory.getLogger(ToolExecutionService.class);

    private final ToolOrchestratorService orchestrator;
    private final DocumentUnderstandingService documents;
    private final UniversalDataAnalysisService analysis;
    private final ChartGenerationService charts;
    private final ChartReadingService chartReader;

    public ToolExecutionService(
            ToolOrchestratorService orchestrator,
            DocumentUnderstandingService documents,
            UniversalDataAnalysisService analysis,
            ChartGenerationService charts,
            ChartReadingService chartReader
    ) {
        this.orchestrator = orchestrator;
        this.documents = documents;
        this.analysis = analysis;
        this.charts = charts;
        this.chartReader = chartReader;
    }

    public Map<String, Object> execute(
            String userRequest,
            MultipartFile document,
            MultipartFile image,
            Map<String, Object> dataset,
            String xColumn,
            String yColumn,
            String chartType,
            String aggregation
    ) {

        boolean hasDocument =
                document != null && !document.isEmpty();

        boolean hasImage =
                image != null && !image.isEmpty();

        boolean hasDataset = dataset != null
                && dataset.get("rows") instanceof List<?>;

        Map<String, Object> plan = orchestrator.plan(
                userRequest,
                hasDocument,
                hasDataset,
                hasImage
        );

        List<?> selectedTools =
                (List<?>) plan.get("tools");

        List<Map<String, Object>> executions =
                new ArrayList<>();

        Map<String, Object> workingDataset = dataset;

        for (Object selected : selectedTools) {

            String tool = String.valueOf(selected)
                    .toUpperCase(Locale.ROOT);

            try {

                Object output;

                switch (tool) {

                    case "DOCUMENT_UNDERSTANDING" -> {

                        if (!hasDocument) {
                            continue;
                        }

                        output = documents.understand(document);

                        if (output instanceof Map<?, ?> result
                                && result.get("dataset") instanceof Map<?, ?>) {

                            @SuppressWarnings("unchecked")
                            Map<String, Object> extracted =
                                    (Map<String, Object>)
                                            result.get("dataset");

                            workingDataset = extracted;
                        }
                    }

                    case "DATA_ANALYSIS" -> {

                        if (workingDataset == null) {
                            continue;
                        }

                        output = analysis.analyze(
                                workingDataset
                        );
                    }

                    case "CHART_GENERATION" -> {

                        if (workingDataset == null) {
                            continue;
                        }

                        if (xColumn == null
                                || yColumn == null) {

                            throw new IllegalArgumentException(
                                    "Chart columns are required."
                            );
                        }

                        output = charts.generate(
                                workingDataset,
                                xColumn,
                                yColumn,
                                chartType,
                                aggregation
                        );
                    }

                    case "CHART_READING" -> {

                        if (!hasImage) {
                            continue;
                        }

                        output = chartReader.readChart(
                                image,
                                userRequest
                        );
                    }

                    case "GENERAL_CHAT",
                         "WEB_SEARCH",
                         "DOCUMENT_RETRIEVAL",
                         "CALCULATOR",
                         "DATE_TIME" -> {

                        output = Map.of(
                                "status", "NOT_CONNECTED",
                                "message",
                                "This tool remains handled " +
                                        "by the existing chat system."
                        );
                    }

                    default -> {
                        continue;
                    }
                }

                Map<String, Object> execution =
                        new LinkedHashMap<>();

                execution.put("tool", tool);
                execution.put("status", "SUCCESS");
                execution.put("result", output);

                executions.add(execution);

            } catch (Exception e) {

                log.warn(
                        "Tool execution failed: {}",
                        tool,
                        e
                );

                Map<String, Object> failure =
                        new LinkedHashMap<>();

                failure.put("tool", tool);
                failure.put("status", "FAILED");
                failure.put("error", e.getMessage());

                executions.add(failure);
            }
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("request", userRequest);
        response.put("plan", plan);
        response.put("executions", executions);

        return response;
    }
}
