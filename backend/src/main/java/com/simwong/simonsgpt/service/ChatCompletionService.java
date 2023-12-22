package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatCompletionService {
    private final OpenAIClient openAIClient;

    public Flux<String> chat(Mono<List<ChatMessage>> chatPostRequestMono) {
        return chatPostRequestMono.flatMapMany(openAIClient::chat);
    }
}
