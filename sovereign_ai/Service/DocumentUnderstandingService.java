
package com.example.sovereign_ai.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentUnderstandingService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DocumentUnderstandingService.class
            );

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private final StructuredDataExtractionService extraction;
    private final ObjectMapper mapper;

    public DocumentUnderstandingService(
            StructuredDataExtractionService extraction,
            ObjectMapper mapper
    ) {
        this.extraction = extraction;
        this.mapper = mapper;
    }

    // =====================================================
    // UNIVERSAL DOCUMENT UNDERSTANDING
    // =====================================================

    public Map<String, Object> understand(
            MultipartFile file
    ) throws Exception {

        validateFile(file);

        String filename = Optional.ofNullable(
                file.getOriginalFilename()
        ).orElse("document");

        String extension = filename
                .toLowerCase(Locale.ROOT);

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("filename", filename);
        result.put("sizeBytes", file.getSize());

        if (extension.endsWith(".pdf")) {

            result.put("type", "PDF");
            result.put("content", extractPdf(file));

        } else if (extension.endsWith(".txt")) {

            result.put("type", "TEXT");
            result.put(
                    "content",
                    new String(
                            file.getBytes(),
                            StandardCharsets.UTF_8
                    )
            );

        } else if (extension.endsWith(".csv")
                || extension.endsWith(".xlsx")
                || extension.endsWith(".xls")
                || extension.endsWith(".json")) {

            result.put("type", "STRUCTURED_DATA");

            Map<String, Object> dataset =
                    extraction.extract(file);

            result.put("dataset", dataset);
            result.put(
                    "content",
                    mapper.writeValueAsString(dataset)
            );

        } else {

            throw new IllegalArgumentException(
                    "Unsupported document format."
            );
        }

        log.info(
                "Document processed: {}",
                filename
        );

        return result;
    }

    // =====================================================
    // PDF TEXT EXTRACTION
    // =====================================================

    private String extractPdf(
            MultipartFile file
    ) throws Exception {

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                return "No extractable text found. " +
                        "The PDF may contain scanned images.";
            }

            return text;
        }
    }

    // =====================================================
    // FILE VALIDATION
    // =====================================================

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload a document."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File exceeds the 20 MB limit."
            );
        }
    }
}
