

package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Service.ChartGenerationService;
import com.example.sovereign_ai.Service.StructuredDataExtractionService;
import com.example.sovereign_ai.Service.UniversalDataAnalysisService;
import com.example.sovereign_ai.Tools.DocumentComparisonTool;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final StructuredDataExtractionService extraction;
    private final UniversalDataAnalysisService analysis;
    private final ChartGenerationService charts;
    private final DocumentComparisonTool documentComparisonTool;

    public AnalysisController(
            StructuredDataExtractionService extraction,
            UniversalDataAnalysisService analysis,
            ChartGenerationService charts,
            DocumentComparisonTool documentComparisonTool
    ) {
        this.extraction = extraction;
        this.analysis = analysis;
        this.charts = charts;
        this.documentComparisonTool = documentComparisonTool;
    }

    // ==========================================
    // UPLOAD STRUCTURED DATASET
    // CSV / XLS / XLSX / JSON
    // ==========================================

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        return ResponseEntity.ok(
                extraction.extract(file)
        );
    }

    // ==========================================
    // ANALYZE STRUCTURED DATASET
    // ==========================================

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @RequestBody Map<String, Object> dataset
    ) {

        return ResponseEntity.ok(
                analysis.analyze(dataset)
        );
    }

    // ==========================================
    // GENERATE CHART FROM JSON DATASET
    // ==========================================

    @PostMapping("/chart")
    public ResponseEntity<?> chart(
            @RequestBody ChartRequest request
    ) {

        return ResponseEntity.ok(
                charts.generate(
                        request.dataset(),
                        request.xColumn(),
                        request.yColumn(),
                        request.chartType(),
                        request.aggregation()
                )
        );
    }

    // ==========================================
    // UPLOAD FILE AND GENERATE CHART
    // ==========================================

    @PostMapping(
            value = "/upload-and-chart",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAndChart(
            @RequestParam("file") MultipartFile file,
            @RequestParam("xColumn") String xColumn,
            @RequestParam("yColumn") String yColumn,
            @RequestParam(
                    value = "chartType",
                    defaultValue = "line"
            ) String chartType,
            @RequestParam(
                    value = "aggregation",
                    defaultValue = "none"
            ) String aggregation
    ) throws Exception {

        Map<String, Object> dataset =
                extraction.extract(file);

        Map<String, Object> chart =
                charts.generate(
                        dataset,
                        xColumn,
                        yColumn,
                        chartType,
                        aggregation
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("status", "SUCCESS");
        response.put("filename", file.getOriginalFilename());
        response.put("dataset", dataset);
        response.put("chart", chart);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // DOCUMENT COMPARISON
    // ==========================================

    @PostMapping(
            value = "/compare-documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> compareDocuments(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam(
                    value = "question",
                    required = false
            ) String question
    ) throws Exception {

        return ResponseEntity.ok(
                documentComparisonTool.compareDocuments(
                        file1,
                        file2,
                        question
                )
        );
    }

    // ==========================================
    // VALIDATION ERROR HANDLING
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(
            IllegalArgumentException exception
    ) {

        Map<String, Object> error =
                new LinkedHashMap<>();

        error.put("status", "ERROR");
        error.put("error", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // ==========================================
    // CHART REQUEST DTO
    // ==========================================

    public record ChartRequest(
            Map<String, Object> dataset,
            String xColumn,
            String yColumn,
            String chartType,
            String aggregation
    ) {}
}
