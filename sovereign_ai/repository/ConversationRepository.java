package com.example.sovereign_ai.repository;

import com.example.sovereign_ai.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    @Query("""
        SELECT DISTINCT c FROM Conversation c
        LEFT JOIN FETCH c.documents
        WHERE c.id = :id
    """)
    Optional<Conversation> findByIdWithDocuments(
            @Param("id") Long id
    );
}