package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Service.ConversationDocumentService;
import com.example.sovereign_ai.Service.ConversationDocumentService.DocumentInfo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations/{conversationId}/documents")
@CrossOrigin(origins = "*")
public class ConversationDocumentController {

    private final ConversationDocumentService service;

    public ConversationDocumentController(
            ConversationDocumentService service
    ) {
        this.service = service;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<?> attach(
            @PathVariable Long conversationId,
            @PathVariable Long documentId
    ) {
        try {
            service.attachDocument(conversationId, documentId);
            return ResponseEntity.ok(
                    Map.of("message", "Document attached successfully")
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<DocumentInfo> list(
            @PathVariable Long conversationId
    ) {
        return service.listDocuments(conversationId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> detach(
            @PathVariable Long conversationId,
            @PathVariable Long documentId
    ) {
        try {
            service.detachDocument(conversationId, documentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}