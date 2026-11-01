
package com.example.sovereign_ai.Agent;

import com.example.sovereign_ai.Service.*;
import com.example.sovereign_ai.Tools.DocumentComparisonTool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ToolRegistry {

    @FunctionalInterface
    public interface ToolAction {
        Object execute(ExecutionContext context)
                throws Exception;
    }

    private final Map<String, ToolAction> tools =
            new LinkedHashMap<>();

    public ToolRegistry(
            DocumentUnderstandingService documents,
            UniversalDataAnalysisService analysis,
            ChartGenerationService charts,
            ChartReadingService chartReader,
            DocumentComparisonTool comparison
    ) {

        register("DOCUMENT_UNDERSTANDING", ctx -> {

            if (!ctx.hasDocument()) {
                throw new IllegalArgumentException(
                        "Document is required."
                );
            }

            Map<String, Object> result =
                    documents.understand(ctx.getDocument());

            if (result.get("dataset") instanceof Map<?, ?>) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dataset =
                        (Map<String, Object>)
                                result.get("dataset");

                ctx.setDataset(dataset);
            }

            return result;
        });

        register("DATA_ANALYSIS", ctx -> {

            if (!ctx.hasDataset()) {
                throw new IllegalArgumentException(
                        "A dataset is required."
                );
            }

            return analysis.analyze(ctx.getDataset());
        });

        register("CHART_GENERATION", ctx -> {

            if (!ctx.hasDataset()) {
                throw new IllegalArgumentException(
                        "A dataset is required."
                );
            }

            if (ctx.getXColumn() == null
                    || ctx.getYColumn() == null) {

                throw new IllegalArgumentException(
                        "xColumn and yColumn are required."
                );
            }

            return charts.generate(
                    ctx.getDataset(),
                    ctx.getXColumn(),
                    ctx.getYColumn(),
                    ctx.getChartType() == null
                            ? "bar" : ctx.getChartType(),
                    ctx.getAggregation() == null
                            ? "none" : ctx.getAggregation()
            );
        });

        register("CHART_READING", ctx -> {

            if (!ctx.hasImage()) {
                throw new IllegalArgumentException(
                        "An image is required."
                );
            }

            return chartReader.readChart(
                    ctx.getImage(),
                    ctx.getRequest()
            );
        });

        register("DOCUMENT_COMPARISON", ctx -> {

            if (!ctx.hasDocument()
                    || !ctx.hasSecondDocument()) {

                throw new IllegalArgumentException(
                        "Two documents are required."
                );
            }

            return comparison.compareDocuments(
                    ctx.getDocument(),
                    ctx.getSecondDocument(),
                    ctx.getRequest()
            );
        });
    }

    public void register(
            String name,
            ToolAction action
    ) {
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Tool already registered: " + name
            );
        }

        tools.put(name, action);
    }

    public boolean contains(String name) {
        return tools.containsKey(name);
    }

    public Object execute(
            String name,
            ExecutionContext context
    ) throws Exception {

        ToolAction action = tools.get(name);

        if (action == null) {
            throw new IllegalArgumentException(
                    "Unknown tool: " + name
            );
        }

        return action.execute(context);
    }

    public Set<String> availableTools() {
        return Set.copyOf(tools.keySet());
    }
}
