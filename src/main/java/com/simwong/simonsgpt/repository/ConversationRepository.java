package com.simwong.simonsgpt.repository;

import com.simwong.simonsgpt.domain.Conversation;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConversationRepository extends ReactiveCrudRepository<Conversation, Integer> {

    @NotNull
    @Override
    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId AND is_deleted = 0")
    Mono<Conversation> findById(@NotNull Integer conversationId);

    @NotNull
    @Query("SELECT * FROM conversations WHERE user_id = :userId AND is_deleted = 0")
    Flux<Conversation> findByUserId(Integer userId);

    @NotNull
    @Query("UPDATE conversations SET title = :#{#conversation.title}, updated_at = CURRENT_TIMESTAMP WHERE conversation_id = :#{#conversation.conversationId} AND is_deleted = 0")
    Mono<Void> updateConversation(Conversation conversation);

    @NotNull
    @Override
    @Query("UPDATE conversations SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE conversation_id = :conversationId AND is_deleted = 0")
    Mono<Void> deleteById(@NotNull Integer conversationId);
}
