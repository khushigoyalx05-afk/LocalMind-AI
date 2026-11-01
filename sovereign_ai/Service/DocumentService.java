
package com.example.sovereign_ai.Service;

import com.example.sovereign_ai.entity.Document;
import com.example.sovereign_ai.entity.DocumentChunk;
import com.example.sovereign_ai.repository.DocumentRepository;
import com.example.sovereign_ai.repository.DocumentChunkRepository;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DocumentService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;
    private static final int VECTOR_BATCH_SIZE = 20;

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorStore vectorStore;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            VectorStore vectorStore
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.vectorStore = vectorStore;
    }

    // ============================================
    // UPLOAD AND PROCESS PDF
    // ============================================

    public Document uploadPdf(MultipartFile file) throws Exception {

        validateFile(file);

        byte[] pdfBytes = file.getBytes();

        List<ChunkData> extractedChunks = new ArrayList<>();

        // ========================================
        // 1. EXTRACT TEXT FROM PDF
        // ========================================

        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {

            if (pdf.getNumberOfPages() == 0) {
                throw new IllegalArgumentException(
                        "PDF contains no pages"
                );
            }

            PDFTextStripper stripper = new PDFTextStripper();

            int chunkIndex = 0;

            for (int pageNumber = 1;
                 pageNumber <= pdf.getNumberOfPages();
                 pageNumber++) {

                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);

                String pageText = stripper
                        .getText(pdf)
                        .trim();

                if (pageText.isBlank()) {
                    continue;
                }

                // ====================================
                // 2. SPLIT PAGE INTO CHUNKS
                // ====================================

                int start = 0;

                while (start < pageText.length()) {

                    int end = Math.min(
                            start + CHUNK_SIZE,
                            pageText.length()
                    );

                    String chunkText = pageText
                            .substring(start, end)
                            .trim();

                    if (!chunkText.isBlank()) {

                        extractedChunks.add(
                                new ChunkData(
                                        chunkIndex++,
                                        pageNumber,
                                        chunkText
                                )
                        );
                    }

                    if (end == pageText.length()) {
                        break;
                    }

                    start = end - CHUNK_OVERLAP;
                }
            }
        }

        // ========================================
        // 3. VALIDATE EXTRACTED CONTENT
        // ========================================

        if (extractedChunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No extractable text found in PDF. " +
                            "Scanned PDFs may require OCR."
            );
        }

        // ========================================
        // 4. SAVE DOCUMENT METADATA IN MYSQL
        // ========================================

        Document document = new Document();

        String fileName = Objects.requireNonNullElse(
                file.getOriginalFilename(),
                "document.pdf"
        );

        document.setFileName(fileName);
        document.setContentType("application/pdf");

        document = documentRepository.save(document);

        // ========================================
        // 5. PREPARE MYSQL AND VECTOR CHUNKS
        // ========================================

        List<DocumentChunk> databaseChunks =
                new ArrayList<>();

        List<org.springframework.ai.document.Document> vectorDocuments =
                new ArrayList<>();

        for (ChunkData data : extractedChunks) {

            DocumentChunk chunk = new DocumentChunk();

            chunk.setDocument(document);
            chunk.setContent(data.content());
            chunk.setChunkIndex(data.index());
            chunk.setPageNumber(data.page());

            databaseChunks.add(chunk);

            org.springframework.ai.document.Document aiDocument =
                    new org.springframework.ai.document.Document(
                            data.content()
                    );

            // Metadata required for multi-PDF retrieval

            aiDocument.getMetadata().put(
                    "documentId",
                    document.getId()
            );

            aiDocument.getMetadata().put(
                    "fileName",
                    document.getFileName()
            );

            aiDocument.getMetadata().put(
                    "chunkIndex",
                    data.index()
            );

            aiDocument.getMetadata().put(
                    "pageNumber",
                    data.page()
            );

            vectorDocuments.add(aiDocument);
        }

        // ========================================
        // 6. SAVE CHUNKS IN MYSQL
        // ========================================

        documentChunkRepository.saveAll(databaseChunks);

        // ========================================
        // 7. GENERATE AND STORE EMBEDDINGS
        // ========================================

        for (int start = 0;
             start < vectorDocuments.size();
             start += VECTOR_BATCH_SIZE) {

            int end = Math.min(
                    start + VECTOR_BATCH_SIZE,
                    vectorDocuments.size()
            );

            List<org.springframework.ai.document.Document> batch =
                    vectorDocuments.subList(start, end);

            vectorStore.add(batch);
        }

        System.out.println(
                "LocalMind AI | PDF uploaded: " +
                        document.getFileName()
        );

        System.out.println(
                "LocalMind AI | Total chunks: " +
                        extractedChunks.size()
        );

        return document;
    }

    // ============================================
    // VALIDATE PDF FILE
    // ============================================

    private void validateFile(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a PDF file"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "PDF must be smaller than 20 MB"
            );
        }

        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF files are supported"
            );
        }

        byte[] header;

        try (var stream = file.getInputStream()) {
            header = stream.readNBytes(5);
        }

        if (header.length != 5
                || header[0] != '%'
                || header[1] != 'P'
                || header[2] != 'D'
                || header[3] != 'F'
                || header[4] != '-') {

            throw new IllegalArgumentException(
                    "Invalid PDF file"
            );
        }
    }

    // ============================================
    // INTERNAL CHUNK DATA
    // ============================================

    private record ChunkData(
            int index,
            int page,
            String content
    ) {
    }
}
