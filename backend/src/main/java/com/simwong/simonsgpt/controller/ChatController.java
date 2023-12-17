package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.api.ChatApi;
import com.simwong.simonsgpt.domain.ChatPostRequest;
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
    public Flux<String> chatPost(Mono<ChatPostRequest> chatPostRequest, ServerWebExchange exchange) {
        return chatService.chat(chatPostRequest);
    }
}
