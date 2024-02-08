package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.domain.Conversation;
import com.simwong.simonsgpt.domain.Message;
import com.simwong.simonsgpt.entity.ChatRequest;
import com.simwong.simonsgpt.entity.UnauthorizedException;
import com.simwong.simonsgpt.repository.ConversationRepository;
import com.simwong.simonsgpt.repository.MessageRepository;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAIClient openAIClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JwtTokenService jwtTokenService;

    @Value("${maxTitleLength}")
    private Integer maxTitleLength;
    private final String defaultSystemPrompt = loadPromptFromFile("prompts/default.txt");
    private final String promptOptimizationPrompt = loadPromptFromFile("prompts/prompt-optimization.txt");

    public Flux<String> sendMessage(Mono<ChatRequest> chatPostRequestMono) {
        // Use a StringBuilder to collect the streamed data
        StringBuilder completeResponse = new StringBuilder();
        List<Message> messagesToBeSaved = new ArrayList<>();

        return chatPostRequestMono
                .flatMapMany(chatRequest -> {
                    List<Message> requestChatMessages = chatRequest.getChatMessages();

                    // Check if enabledPromptOptimization is true and if the request contains only one message
                    if (chatRequest.getEnabledPromptOptimization() != null
                            && chatRequest.getEnabledPromptOptimization()
                            && requestChatMessages.size() == 1) {
                        // Make a call with openAIClient.chat() and wait for it to finish
                        return openAIClient.chat(buildChatMessages(promptOptimizationPrompt, requestChatMessages))
                                .collectList()
                                .flatMapMany(strings -> {
                                    String optimizedPrompt = String.join("", strings);
                                    requestChatMessages.get(0).setContent(optimizedPrompt);
                                    return invokeChat(chatRequest, requestChatMessages, messagesToBeSaved);
                                });
                    }
                    return invokeChat(chatRequest, requestChatMessages, messagesToBeSaved);
                })
                .doOnNext(completeResponse::append)
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signalType -> saveMessage(completeResponse.toString(), messagesToBeSaved).subscribe());
    }

    public Mono<Conversation> createConversation(ServerWebExchange exchange) {
        return Mono.deferContextual(contextView -> {
            String jwt;
            Integer userId;
            try {
                jwt = contextView.get("jwt");
                userId = jwtTokenService.extractValue(jwt, "userId", Integer.class);
                return conversationRepository.save(
                        Conversation.builder()
                                .userId(userId)
                                .build());
            } catch (Exception e) {
                return Mono.error(new UnauthorizedException("未登录", e));
            }
        });
    }

    public Mono<Message> saveMessage(String assistantMessage, List<Message> chatMessages) {
        // Save the last message in the list, which is the message we just received
        int lastIndex = chatMessages.size() - 1;
        if (lastIndex < 0) {
            lastIndex = 0;
        }
        Message message = chatMessages.get(lastIndex);
        if (message.getConversationId() == null) {
            // if the conversation id is null, the user is not login, no need to save the message
            return Mono.empty();
        }
        return conversationRepository.findById(message.getConversationId())
                .flatMap(conversation -> {
                    // check if the conversation has title, if not, set the title to the first message
                    if (StringUtils.isBlank(conversation.getTitle())) {
                        conversation.setTitle(message.getContent().substring(0, Math.min(message.getContent().length(), maxTitleLength)));
                    }
                    return conversationRepository.updateConversation(conversation)
                            .then(Mono.defer(() -> {
                                // set the conversation id to the message and save
                                message.setConversationId(conversation.getConversationId());
                                return messageRepository.save(message)
                                        .flatMap(ignored2 -> {
                                            // then save the message just generated by OpenAI
                                            return messageRepository.save(
                                                    Message.builder()
                                                            .conversationId(message.getConversationId())
                                                            .role(ChatMessageRole.ASSISTANT.value())
                                                            .content(assistantMessage)
                                                            .build());
                                        });
                            }));
                });
    }

    public Flux<Conversation> listConversations(ServerWebExchange exchange) {
        return Flux.deferContextual(contextView -> {
            String jwt;
            Integer userId;
            try {
                jwt = contextView.get("jwt");
                userId = jwtTokenService.extractValue(jwt, "userId", Integer.class);
                return conversationRepository.findByUserId(userId);
            } catch (Exception e) {
                return Flux.error(new UnauthorizedException("未登录", e));
            }
        });
    }

    public Flux<Message> listMessages(Integer conversationId, ServerWebExchange exchange) {
        return Flux.deferContextual(contextView -> {
            String jwt;
            Integer userId;
            try {
                jwt = contextView.get("jwt");
                userId = jwtTokenService.extractValue(jwt, "userId", Integer.class);
                return conversationRepository.findById(conversationId)
                        .flatMapMany(conversation -> {
                            if (!conversation.getUserId().equals(userId)) {
                                return Flux.error(new UnauthorizedException("未登录"));
                            }
                            return messageRepository.findByConversationId(conversationId);
                        });
            } catch (Exception e) {
                return Flux.error(new UnauthorizedException("未登录", e));
            }
        });
    }

    public Mono<Void> deleteConversation(Integer conversationId, ServerWebExchange exchange) {
        return Mono.deferContextual(contextView -> {
            String jwt;
            Integer userId;
            try {
                jwt = contextView.get("jwt");
                userId = jwtTokenService.extractValue(jwt, "userId", Integer.class);
                return conversationRepository.findById(conversationId)
                        .flatMap(conversation -> {
                            if (!conversation.getUserId().equals(userId)) {
                                return Mono.error(new UnauthorizedException("无权限"));
                            }
                            return Mono.empty();
                        })
                        .then(messageRepository.deleteByConversationId(conversationId))
                        .then(conversationRepository.deleteById(conversationId));
            } catch (Exception e) {
                return Mono.error(new UnauthorizedException("未登录", e));
            }
        });
    }

    private String loadPromptFromFile(String filePath) {
        try {
            // Assuming the file is located directly under the resources directory
            Path path = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(filePath)).toURI());
            return Files.readString(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompt from file", e);
        }
    }

    private List<ChatMessage> buildChatMessages(String systemMessage, List<Message> userMessage) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (systemMessage != null) {
            chatMessages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage, "Simon"));
        }
        userMessage.forEach(message -> chatMessages.add(new ChatMessage(message.getRole(), message.getContent())));
        return chatMessages;
    }

    private Flux<String> invokeChat(ChatRequest chatRequest, List<Message> requestChatMessages, List<Message> messagesToBeSaved) {
        Integer conversationId = chatRequest.getConversationId();
        if (conversationId == null) {
            // if the conversation id is null, the user is not login, skip to the invoke of OpenAI
            return openAIClient.chat(buildChatMessages(defaultSystemPrompt, requestChatMessages));
        }
        requestChatMessages.forEach(message -> message.setConversationId(conversationId));
        messagesToBeSaved.addAll(requestChatMessages);
        return openAIClient.chat(buildChatMessages(defaultSystemPrompt, requestChatMessages));
    }
}
