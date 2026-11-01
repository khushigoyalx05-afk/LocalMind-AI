
package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Agent.ExecutionContext;
import com.example.sovereign_ai.Service.UnifiedOrchestratorService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final UnifiedOrchestratorService orchestrator;
    private final ObjectMapper mapper;

    public AgentController(
            UnifiedOrchestratorService orchestrator,
            ObjectMapper mapper
    ) {
        this.orchestrator = orchestrator;
        this.mapper = mapper;
    }

    @PostMapping(
            value = "/execute",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> execute(
            @RequestParam("request") String request,

            @RequestParam(
                    value = "document",
                    required = false
            ) MultipartFile document,

            @RequestParam(
                    value = "secondDocument",
                    required = false
            ) MultipartFile secondDocument,

            @RequestParam(
                    value = "image",
                    required = false
            ) MultipartFile image,

            @RequestParam(
                    value = "dataset",
                    required = false
            ) String datasetJson,

            @RequestParam(
                    value = "xColumn",
                    required = false
            ) String xColumn,

            @RequestParam(
                    value = "yColumn",
                    required = false
            ) String yColumn,

            @RequestParam(
                    value = "chartType",
                    required = false
            ) String chartType,

            @RequestParam(
                    value = "aggregation",
                    required = false
            ) String aggregation

    ) throws Exception {

        Map<String, Object> dataset = null;

        if (datasetJson != null
                && !datasetJson.isBlank()) {

            dataset = mapper.readValue(
                    datasetJson,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        ExecutionContext context =
                new ExecutionContext(
                        request,
                        document,
                        secondDocument,
                        image,
                        dataset,
                        xColumn,
                        yColumn,
                        chartType,
                        aggregation
                );

        return ResponseEntity.ok(
                orchestrator.execute(context)
        );
    }
}
