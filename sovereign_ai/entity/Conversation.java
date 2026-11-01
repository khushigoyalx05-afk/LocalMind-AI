package com.example.sovereign_ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class Conversation {

    public enum ChatMode {
        GENERAL, DOCUMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_mode", nullable = false)
    private ChatMode chatMode = ChatMode.GENERAL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversation_documents",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id")
    )
    @JsonIgnore
    private Set<Document> documents = new LinkedHashSet<>();

    public Conversation() {}

    public Conversation(String title) {
        this.title = title;
    }

    public Conversation(String title, ChatMode chatMode) {
        this.title = title;
        this.chatMode = chatMode == null
                ? ChatMode.GENERAL : chatMode;
    }

    public Long getId() { return id; }

    public String getTitle() { return title; }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChatMode getChatMode() { return chatMode; }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode == null
                ? ChatMode.GENERAL : chatMode;
    }

    public Set<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents == null
                ? new LinkedHashSet<>()
                : documents;
    }
}