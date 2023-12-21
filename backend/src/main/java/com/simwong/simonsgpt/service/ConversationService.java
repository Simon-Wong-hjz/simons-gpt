package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.domain.Conversation;
import com.simwong.simonsgpt.repo.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;

    // Constructor with ConversationRepository

    public Flux<Conversation> getConversationsByUserId(Long userId) {
        return conversationRepository.findByUserId(userId);
    }

    // Methods to save and retrieve conversation data
}
