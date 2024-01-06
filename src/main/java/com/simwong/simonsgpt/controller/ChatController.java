package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.api.ChatApi;
import com.simwong.simonsgpt.domain.Conversation;
import com.simwong.simonsgpt.domain.Message;
import com.simwong.simonsgpt.entity.ChatRequest;
import com.simwong.simonsgpt.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ChatService chatService;

    @Override
    public Flux<String> sendMessage(Mono<ChatRequest> chatRequestMono, ServerWebExchange exchange) {
        return chatService.sendMessage(chatRequestMono);
    }

    @Override
    public Mono<Conversation> createConversation(ServerWebExchange exchange) {
        return chatService.createConversation(exchange);
    }

    @Override
    public Flux<Conversation> listConversations(ServerWebExchange exchange) {
        return chatService.listConversations(exchange);
    }

    @Override
    public Mono<Void> deleteConversation(Integer conversationId, ServerWebExchange exchange) {
        return chatService.deleteConversation(conversationId, exchange);
    }

    @Override
    public Flux<Message> listMessages(Integer conversationId, ServerWebExchange exchange) {
        return chatService.listMessages(conversationId, exchange);
    }
}
