package com.example.sovereign_ai.Service;

import com.example.sovereign_ai.entity.Conversation;
import com.example.sovereign_ai.entity.Document;
import com.example.sovereign_ai.repository.ConversationRepository;
import com.example.sovereign_ai.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationDocumentService {

    private final ConversationRepository conversationRepository;
    private final DocumentRepository documentRepository;

    public ConversationDocumentService(
            ConversationRepository conversationRepository,
            DocumentRepository documentRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public void attachDocument(
            Long conversationId,
            Long documentId
    ) {
        Conversation conversation = getConversation(conversationId);

        if (conversation.getChatMode()
                != Conversation.ChatMode.DOCUMENT) {
            throw new IllegalArgumentException(
                    "PDFs can only be attached to DOCUMENT conversations."
            );
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found: " + documentId
                        )
                );

        boolean alreadyAttached = conversation.getDocuments()
                .stream()
                .anyMatch(d -> d.getId().equals(documentId));

        if (!alreadyAttached) {
            conversation.getDocuments().add(document);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentInfo> listDocuments(Long conversationId) {
        return getConversation(conversationId)
                .getDocuments()
                .stream()
                .map(d -> new DocumentInfo(
                        d.getId(),
                        d.getFileName(),
                        d.getContentType()
                ))
                .toList();
    }

    @Transactional
    public void detachDocument(
            Long conversationId,
            Long documentId
    ) {
        Conversation conversation = getConversation(conversationId);

        boolean removed = conversation.getDocuments()
                .removeIf(d -> d.getId().equals(documentId));

        if (!removed) {
            throw new IllegalArgumentException(
                    "Document is not attached to this conversation."
            );
        }
    }

    private Conversation getConversation(Long id) {
        return conversationRepository.findByIdWithDocuments(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conversation not found: " + id
                        )
                );
    }

    public record DocumentInfo(
            Long id,
            String fileName,
            String contentType
    ) {}
}