
package com.example.sovereign_ai.Tools;

import com.example.sovereign_ai.Service.DocumentChatService;
import com.example.sovereign_ai.entity.Conversation;
import com.example.sovereign_ai.repository.ConversationRepository;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DataAnalysisTool {

    private final Long conversationId;
    private final ConversationRepository conversationRepository;
    private final DocumentChatService documentChatService;

    public DataAnalysisTool(
            Long conversationId,
            ConversationRepository conversationRepository,
            DocumentChatService documentChatService
    ) {
        this.conversationId = conversationId;
        this.conversationRepository = conversationRepository;
        this.documentChatService = documentChatService;
    }

    @Tool(description = """
            Search PDFs attached to the current conversation.
            Returns relevant passages with filenames and page numbers.
            Use this tool to answer questions about uploaded documents,
            locate specific topics, or find supporting evidence.
            """)
    public String searchPdf(
            @ToolParam(description =
                    "The question or topic to search for in the PDFs")
            String query
    ) {

        Conversation conversation = conversationRepository
                .findByIdWithDocuments(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conversation not found"
                        )
                );

        DocumentChatService.RetrievalResult result =
                documentChatService.retrieve(
                        conversation,
                        query
                );

        return result.context();
    }
}
