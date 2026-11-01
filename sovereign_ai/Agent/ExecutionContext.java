
package com.example.sovereign_ai.Agent;

import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionContext {

    private final String request;
    private final MultipartFile document;
    private final MultipartFile secondDocument;
    private final MultipartFile image;

    private Map<String, Object> dataset;

    private final Map<String, Object> results =
            new LinkedHashMap<>();

    private final String xColumn;
    private final String yColumn;
    private final String chartType;
    private final String aggregation;

    public ExecutionContext(
            String request,
            MultipartFile document,
            MultipartFile secondDocument,
            MultipartFile image,
            Map<String, Object> dataset,
            String xColumn,
            String yColumn,
            String chartType,
            String aggregation
    ) {
        this.request = request;
        this.document = document;
        this.secondDocument = secondDocument;
        this.image = image;
        this.dataset = dataset;
        this.xColumn = xColumn;
        this.yColumn = yColumn;
        this.chartType = chartType;
        this.aggregation = aggregation;
    }

    public String getRequest() {
        return request;
    }

    public MultipartFile getDocument() {
        return document;
    }

    public MultipartFile getSecondDocument() {
        return secondDocument;
    }

    public MultipartFile getImage() {
        return image;
    }

    public Map<String, Object> getDataset() {
        return dataset;
    }

    public void setDataset(Map<String, Object> dataset) {
        this.dataset = dataset;
    }

    public String getXColumn() {
        return xColumn;
    }

    public String getYColumn() {
        return yColumn;
    }

    public String getChartType() {
        return chartType;
    }

    public String getAggregation() {
        return aggregation;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public void addResult(String tool, Object result) {
        results.put(tool, result);
    }

    public boolean hasDocument() {
        return document != null && !document.isEmpty();
    }

    public boolean hasSecondDocument() {
        return secondDocument != null
                && !secondDocument.isEmpty();
    }

    public boolean hasImage() {
        return image != null && !image.isEmpty();
    }

    public boolean hasDataset() {
        return dataset != null
                && dataset.get("rows") instanceof java.util.List<?>;
    }
}
