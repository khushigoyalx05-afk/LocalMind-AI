
package com.example.sovereign_ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(
        name = "document_chunks",
        indexes = {
                @Index(
                        name = "idx_document_chunk_document",
                        columnList = "document_id"
                )
        }
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false)
    private Integer pageNumber;

    public DocumentChunk() {
    }

    public DocumentChunk(
            Document document,
            String content,
            Integer chunkIndex,
            Integer pageNumber
    ) {
        this.document = document;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}
