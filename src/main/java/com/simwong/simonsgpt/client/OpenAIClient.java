package com.simwong.simonsgpt.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simwong.simonsgpt.domain.Message;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Value("${openai.connectTimeout}")
    private int connectTimeoutMis;

    @Value("${openai.socketTimeout}")
    private int socketTimeoutMis;

    private Duration timeoutDuration;

    private static final String OPENAI_BETA = "OpenAI-Beta";
    private static final String ASSISTANT_V1 = "assistants=v1";

    private final ObjectMapper objectMapper;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        timeoutDuration = Duration.ofMillis(socketTimeoutMis + 500);
        ConnectionProvider connectionProvider =
                ConnectionProvider.builder("OpenAI").maxIdleTime(Duration.ofMillis(60_000))
                        .fifo()
                        .pendingAcquireTimeout(Duration.ofMillis(connectTimeoutMis)).build();
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper)))
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create(connectionProvider).compress(true)
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMis)
                                .doOnConnected(connection -> connection
                                        .addHandlerLast(new ReadTimeoutHandler(socketTimeoutMis, TimeUnit.MILLISECONDS)))))
                .build();
    }

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
                        OpenAiResponse<Assistant> assistantsResponse = objectMapper.readValue(res, new TypeReference<>() {
                        });
                        return Flux.fromIterable(assistantsResponse.getData());
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }

    public Flux<String> chat(@NotNull List<ChatMessage> chatMessages) {
        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                .model("gpt-4-1106-preview")
                .stream(true)
                .messages(chatMessages)
                .build();
        log.info("Calling OpenAI to chat: {}", chatRequest);
        ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {
        };
        return webClient.post()
                .uri(openAiEndpoint + "/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
//                .header(OPENAI_BETA, ASSISTANT_V1)
                .bodyValue(chatRequest)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenAI call failed with status: {}, error body: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("OpenAI call failed with status: " + clientResponse.statusCode()));
                                }))
                .bodyToFlux(type)
                .flatMap(serverSentEvent -> {
                    String data = serverSentEvent.data();
                    try {
                        ChatCompletionChunk chatCompletionChunk = objectMapper.readValue(data, ChatCompletionChunk.class);
                        if (chatCompletionChunk.getChoices() != null && !chatCompletionChunk.getChoices().isEmpty()) {
                            // Handle regular ChatResponse
                            if (chatCompletionChunk.getChoices().get(0).getMessage() != null) {

                                log.info("OpenAI call succeeded with response: {}", chatCompletionChunk);
                                String content = chatCompletionChunk.getChoices().get(0).getMessage().getContent();
                                return Mono.justOrEmpty(content);
                            } else if (StringUtils.isNotEmpty(chatCompletionChunk.getChoices().get(0).getFinishReason())) {
                                log.info("OpenAI call finished with reason: {}", chatCompletionChunk.getChoices().get(0).getFinishReason());
                                return Mono.empty();
                            } else {
                                log.info("Unknown response from OpenAI: {}", chatCompletionChunk);
                                return Mono.empty();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("OpenAI didn't return valid response: {}", data);
                        return Mono.empty();
                    }
                    return Mono.empty();
                })
                .timeout(timeoutDuration)
                .doOnError(e -> log.error("Error calling OpenAI to chat", e));
    }
}
