
package com.example.sovereign_ai.Service;

import com.example.sovereign_ai.Agent.ExecutionContext;
import com.example.sovereign_ai.Agent.ToolRegistry;
import com.example.sovereign_ai.Agent.ToolResult;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UnifiedOrchestratorService {

    private final ToolOrchestratorService planner;
    private final ToolRegistry registry;

    public UnifiedOrchestratorService(
            ToolOrchestratorService planner,
            ToolRegistry registry
    ) {
        this.planner = planner;
        this.registry = registry;
    }

    public Map<String, Object> execute(
            ExecutionContext context
    ) {

        Map<String, Object> plan = planner.plan(
                context.getRequest(),
                context.hasDocument(),
                context.hasDataset(),
                context.hasImage()
        );

        Set<String> selected = new LinkedHashSet<>();

        Object plannedTools = plan.get("tools");

        if (plannedTools instanceof List<?> list) {

            for (Object item : list) {
                String name = String.valueOf(item);

                if (registry.contains(name)) {
                    selected.add(name);
                }
            }
        }

        // Automatically include document comparison
        // when two files are supplied.
        if (context.hasDocument()
                && context.hasSecondDocument()) {
            selected.add("DOCUMENT_COMPARISON");
        }

        // Extract structured data before analysis.
        if (context.hasDocument()
                && !context.hasDataset()
                && (selected.contains("DATA_ANALYSIS")
                || selected.contains("CHART_GENERATION"))) {

            selected.add("DOCUMENT_UNDERSTANDING");
        }

        // Ensure extraction runs before analysis/charts.
        List<String> executionOrder = List.of(
                "DOCUMENT_UNDERSTANDING",
                "DOCUMENT_COMPARISON",
                "DATA_ANALYSIS",
                "CHART_GENERATION",
                "CHART_READING"
        );

        List<ToolResult> results = new ArrayList<>();

        for (String tool : executionOrder) {

            if (!selected.contains(tool)) {
                continue;
            }

            long start = System.nanoTime();

            try {

                Object output = registry.execute(
                        tool,
                        context
                );

                context.addResult(tool, output);

                results.add(
                        ToolResult.success(
                                tool,
                                output,
                                (System.nanoTime() - start)
                                        / 1_000_000
                        )
                );

            } catch (Exception e) {

                results.add(
                        ToolResult.failure(
                                tool,
                                e.getMessage(),
                                (System.nanoTime() - start)
                                        / 1_000_000
                        )
                );
            }
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("request", context.getRequest());
        response.put("plan", plan);
        response.put("executions", results);
        response.put("availableTools", registry.availableTools());

        return response;
    }
}
