package com.simwong.simonsgpt.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simwong.simonsgpt.domain.AssistantsResponse;
import com.simwong.simonsgpt.domain.ChatRequest;
import com.simwong.simonsgpt.domain.ChatResponse;
import com.simwong.simonsgpt.model.Assistant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {
    @Value("${openai.endpoint}")
    private String openAiEndpoint;

    @Value("${openai.key}")
    private String openAiKey;

    @Value("${openai.retry}")
    private int openAiRetry;

    private static final String OPENAI_BETA = "OpenAI-Beta";
    private static final String ASSISTANT_V1 = "assistants=v1";

    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder().build();

    public Flux<Assistant> listAssistants() {
        log.info("Calling OpenAI to list assistants");
        return webClient.get()
                .uri(openAiEndpoint + "/assistants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                .header(OPENAI_BETA, ASSISTANT_V1)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnError(e -> {
                            log.error("Error calling OpenAI to list assistants", e);
                            throw new RuntimeException(e);
                        })
                        .doOnSuccess(res -> log.info("call list assistants response: {}", res)))
                .flatMapMany(res -> {
                    try {
                        AssistantsResponse assistantsResponse = objectMapper.readValue(res, AssistantsResponse.class);
                        return Flux.fromIterable(assistantsResponse.getData());
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }

    public Mono<String> chat(String message) {
        ChatRequest chatRequest = ChatRequest.builder()
                .model("gpt-4-1106-preview")
                .messages(
                        List.of(ChatRequest.Messages.builder()
                                        .role("system")
                                        .content("你是一个助手。你可以结合自身的知识与用户的需求解答用户的问题。除非用户要求你使用中文以外的语言，否则你只使用中文回答问题。")
                                        .build()
                                , ChatRequest.Messages.builder()
                                        .role("user")
                                        .content(message)
                                        .build()))
                .build();

        log.info("Calling OpenAI to chat: {}", message);
        return webClient.post()
                .uri(openAiEndpoint + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                .header(OPENAI_BETA, ASSISTANT_V1)
                .bodyValue(chatRequest)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                        .doOnError(e -> {
                            log.error("Error calling OpenAI to chat", e);
                            throw new RuntimeException(e);
                        })
                        .doOnSuccess(res -> log.info("call chat response: {}", res)))
                .flatMap(res -> {
                    try {
                        ChatResponse chatResponse = objectMapper.readValue(res, ChatResponse.class);
                        return Mono.just(chatResponse.getChoices().get(0).getMessage().getContent());
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }
}
