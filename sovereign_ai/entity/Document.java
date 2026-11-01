
package com.example.sovereign_ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    public Document() {
    }

    public Document(
            String fileName,
            String contentType,
            String extractedText
    ) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.extractedText = extractedText;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
}
