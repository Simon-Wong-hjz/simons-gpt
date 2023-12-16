package com.simwong.simonsgpt.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simwong.simonsgpt.domain.AssistantsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    public Mono<AssistantsResponse> listAssistants() {
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
                .flatMap(res -> {
                    try {
                        return Mono.just(objectMapper.readValue(res, AssistantsResponse.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }
}
