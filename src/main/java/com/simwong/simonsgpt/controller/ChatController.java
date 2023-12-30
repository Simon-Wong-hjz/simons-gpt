package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.api.ChatApi;
import com.simwong.simonsgpt.service.ChatService;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final ChatService chatService;

    @Override
    public Flux<String> chatPost(Mono<List<ChatMessage>> chatMessages, ServerWebExchange exchange) {
        return chatService.chat(chatMessages);
    }
}
