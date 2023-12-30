package com.simwong.simonsgpt.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simwong.simonsgpt.domain.AssistantsResponse;
import com.simwong.simonsgpt.domain.ChatRequest;
import com.simwong.simonsgpt.domain.ChatResponse;
import com.simwong.simonsgpt.model.Assistant;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
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

    public Flux<String> chat(List<ChatMessage> chatMessages) {
        ArrayList<ChatRequest.Messages> messages = new ArrayList<>();
        messages.add(ChatRequest.Messages.builder()
                .role("system")
                .content("你是一个助手。你可以结合自身的知识与用户的需求解答用户的问题。除非用户要求你使用中文以外的语言，否则你只使用中文回答问题。")
                .build());
        ChatRequest chatRequest = ChatRequest.builder()
                .model("gpt-4-1106-preview")
                .stream(true)
                .build();

        chatMessages.forEach(chatMessage -> {
            messages.add(ChatRequest.Messages.builder()
                    .role(chatMessage.getRole())
                    .content(chatMessage.getContent())
                    .build());
        });
        chatRequest.setMessages(messages);
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
                        ChatResponse chatResponse = objectMapper.readValue(data, ChatResponse.class);
                        if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                            // Handle regular ChatResponse
                            if (chatResponse.getChoices().get(0).getDelta() != null) {

                                log.info("OpenAI call succeeded with response: {}", chatResponse);
                                String content = chatResponse.getChoices().get(0).getDelta().getContent();
                                return Mono.justOrEmpty(content);
                            } else if (StringUtils.isNotEmpty(chatResponse.getChoices().get(0).getFinishReason())) {
                                log.info("OpenAI call finished with reason: {}", chatResponse.getChoices().get(0).getFinishReason());
                                return Mono.empty();
                            } else {
                                log.info("Unknown response from OpenAI: {}", chatResponse);
                                return Mono.empty();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("OpenAI didn't return valid response: {}", data);
                        return Mono.empty();
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("Error calling OpenAI to chat", e));
    }
}
