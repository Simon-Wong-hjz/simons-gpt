package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.model.ChatPostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAIClient openAIClient;

    public Mono<ChatPostRequest> chat(Mono<ChatPostRequest> chatPostRequestMono) {
        return chatPostRequestMono.flatMap(chatPostRequest ->
                openAIClient.chat(chatPostRequest.getMessage())
                        .map(chatResponse -> ChatPostRequest.builder()
                                .message(chatResponse)
                                .build()));
    }
}