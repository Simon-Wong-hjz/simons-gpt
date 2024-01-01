package com.simwong.simonsgpt.repository;

import com.simwong.simonsgpt.domain.Message;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository extends ReactiveCrudRepository<Message, Integer> {
    @NotNull
    @Override
    @Query("SELECT * FROM messages WHERE message_id = :messageId AND is_deleted = 0")
    Mono<Message> findById(@NotNull Integer messageId);

    @NotNull
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId AND is_deleted = 0")
    Flux<Message> findByConversationId(Integer conversationId);

    @NotNull
    @Override
    @Query("UPDATE messages SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE message_id = :messageId AND is_deleted = 0")
    Mono<Void> deleteById(@NotNull Integer messageId);
}
