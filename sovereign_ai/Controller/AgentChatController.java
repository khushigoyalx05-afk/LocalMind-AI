

package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Agent.ExecutionContext;
import com.example.sovereign_ai.Service.ChatService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public AgentChatController(
            ChatService chatService,
            ObjectMapper objectMapper
    ) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            value = "/conversations/{conversationId}/chat",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> chatWithAgent(
            @PathVariable Long conversationId,

            @RequestParam("message") String message,

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

        if (datasetJson != null && !datasetJson.isBlank()) {
            dataset = objectMapper.readValue(
                    datasetJson,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        ExecutionContext context = new ExecutionContext(
                message,
                document,
                secondDocument,
                image,
                dataset,
                xColumn,
                yColumn,
                chartType,
                aggregation
        );

        return chatService.chatWithAgent(
                conversationId,
                context
        );
    }
}
