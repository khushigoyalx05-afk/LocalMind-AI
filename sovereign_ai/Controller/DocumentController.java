

package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Service.DocumentService;
import com.example.sovereign_ai.entity.Document;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService
    ) {
        this.documentService = documentService;
    }

    // ============================================
    // UPLOAD PDF
    // ============================================

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file
    ) {

        try {

            Document document =
                    documentService.uploadPdf(file);

            return ResponseEntity.ok(document);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "error",
                                    "PDF processing failed. Please try again."
                            )
                    );
        }
    }
}
