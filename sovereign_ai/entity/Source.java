
package com.example.sovereign_ai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String fileName;

    private Integer chunkIndex;

    @Column(nullable = false)
    private Integer pageNumber;

    public Source() {
    }

    public Source(
            String fileName,
            Integer chunkIndex,
            Integer pageNumber
    ) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
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
