

package com.example.sovereign_ai.Tools;

import com.example.sovereign_ai.Service.ChartGenerationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Component
public class ChartTool {

    private static final Logger log =
            LoggerFactory.getLogger(ChartTool.class);

    private final ChartGenerationService chartService;

    public ChartTool(
            ChartGenerationService chartService
    ) {
        this.chartService = chartService;
    }

    // =====================================================
    // UNIVERSAL CHART GENERATION
    // =====================================================

    @Tool(
            name = "generate_chart",
            description = """
            Generate structured chart data for visualizing
            datasets from any domain.

            Supported chart types:
            bar, line, pie, scatter.

            Supported aggregations:
            none, sum, average, count.

            Use this tool when the user requests a chart,
            graph, plot, or data visualization.

            The input must contain actual dataset values.
            Never invent numerical data or categories.

            Returns a structured chart specification
            that the frontend can render.
            """
    )
    public Map<String, Object> generateChart(

            @ToolParam(
                    description = "Dataset column names"
            )
            List<String> columns,

            @ToolParam(
                    description = "Dataset rows containing actual values"
            )
            List<Map<String, Object>> rows,

            @ToolParam(
                    description = "Column used for the X axis"
            )
            String xColumn,

            @ToolParam(
                    description = "Column used for the Y axis"
            )
            String yColumn,

            @ToolParam(
                    description = "Chart type: bar, line, pie, scatter"
            )
            String chartType,

            @ToolParam(
                    description = "Aggregation: none, sum, average, count"
            )
            String aggregation
    ) {

        log.info(
                "Chart requested: type={}, x={}, y={}",
                chartType,
                xColumn,
                yColumn
        );

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chart columns cannot be empty."
            );
        }

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chart dataset cannot be empty."
            );
        }

        if (xColumn == null || yColumn == null) {
            throw new IllegalArgumentException(
                    "Both X and Y columns are required."
            );
        }

        Map<String, Object> dataset =
                new LinkedHashMap<>();

        dataset.put("columns", columns);
        dataset.put("rows", rows);
        dataset.put("rowCount", rows.size());

        Map<String, Object> result =
                chartService.generate(
                        dataset,
                        xColumn,
                        yColumn,
                        chartType,
                        aggregation
                );

        log.info(
                "Chart generated successfully with {} points",
                ((List<?>) result.get("data")).size()
        );

        return result;
    }
}
