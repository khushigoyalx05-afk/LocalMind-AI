
package com.example.sovereign_ai.Controller;

import com.example.sovereign_ai.Service.ChatService;
import com.example.sovereign_ai.entity.ChatMessage;
import com.example.sovereign_ai.entity.Conversation;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations")
    public Conversation createConversation(
            @RequestParam(defaultValue = "New Chat") String title,
            @RequestParam(defaultValue = "GENERAL") String mode
    ) {
        return chatService.createConversation(title, mode);
    }

    @GetMapping("/conversations")
    public List<Conversation> getAllConversations() {
        return chatService.getAllConversations();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessage> getMessages(
            @PathVariable Long conversationId
    ) {
        return chatService.getMessages(conversationId);
    }

    @PostMapping(
            value = "/conversations/{conversationId}/chat",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Flux<String> chat(
            @PathVariable Long conversationId,
            @RequestBody String message
    ) {
        return chatService.chat(conversationId, message);
    }

    @PutMapping("/conversations/{conversationId}")
    public Conversation renameConversation(
            @PathVariable Long conversationId,
            @RequestParam String title
    ) {
        return chatService.updateConversationTitle(
                conversationId,
                title
        );
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            @PathVariable Long conversationId
    ) {
        chatService.deleteConversation(conversationId);
    }
}