package com.simwong.simonsgpt.repo;

import com.simwong.simonsgpt.domain.Conversation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ConversationRepository extends ReactiveCrudRepository<Conversation, Long> {
    Flux<Conversation> findByUserId(Long userId);
}
