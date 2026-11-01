package com.example.sovereign_ai.repository;

import com.example.sovereign_ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {
}