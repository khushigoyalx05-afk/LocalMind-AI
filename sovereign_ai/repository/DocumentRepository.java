package com.example.sovereign_ai.repository;

import com.example.sovereign_ai.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {
}