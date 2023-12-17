package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.domain.ChatPostRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAIClient openAIClient;

    public Flux<String> chat(Mono<ChatPostRequest> chatPostRequestMono) {
        return chatPostRequestMono.flatMapMany(chatPostRequest ->
                openAIClient.chat(chatPostRequest.getMessage()));
    }
}
