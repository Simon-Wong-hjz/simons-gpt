package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.domain.Conversation;
import com.simwong.simonsgpt.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @GetMapping("/{userId}")
    public Flux<Conversation> getConversationsByUserId(@PathVariable Long userId) {
        return conversationService.getConversationsByUserId(userId);
    }

    // Endpoints to handle conversation data
}
