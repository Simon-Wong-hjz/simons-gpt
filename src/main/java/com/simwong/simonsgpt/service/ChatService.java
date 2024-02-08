package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.domain.Conversation;
import com.simwong.simonsgpt.domain.Message;
import com.simwong.simonsgpt.entity.ChatRequest;
import com.simwong.simonsgpt.entity.UnauthorizedException;
import com.simwong.simonsgpt.repository.ConversationRepository;
import com.simwong.simonsgpt.repository.MessageRepository;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final OpenAIClient openAIClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JwtTokenService jwtTokenService;

    public Flux<String> sendMessage(Mono<ChatRequest> chatPostRequestMono) {
        // Use a StringBuilder to collect the streamed data
        StringBuilder completeResponse = new StringBuilder();
        List<Message> chatMessages = new ArrayList<>();

        return chatPostRequestMono
                .flatMapMany(chatRequest -> {
                    Integer conversationId = chatRequest.getConversationId();
                    if (conversationId == null) {
                        // if the conversation id is null, the user is not login, skip to the invoke of OpenAI
                        return openAIClient.chat(chatRequest.getChatMessages());
                    }
                    chatRequest.getChatMessages().forEach(message -> message.setConversationId(conversationId));
                    chatMessages.addAll(chatRequest.getChatMessages());
                    return openAIClient.chat(chatRequest.getChatMessages());
                })
                .doOnNext(completeResponse::append)
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signalType -> saveMessage(completeResponse.toString(), chatMessages).subscribe());
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
                        conversation.setTitle(message.getContent());
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
}
