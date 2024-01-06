package com.simwong.simonsgpt.repository;

import com.simwong.simonsgpt.domain.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Integer> {

    @NotNull
    @Query("SELECT * FROM users WHERE username = :username AND is_deleted = 0")
    Mono<User> findByUsername(@NotNull String username);

    @NotNull
    @Override
    @Query("SELECT * FROM users WHERE user_id = :integer AND is_deleted = 0")
    Mono<User> findById(@NotNull Integer integer);

    @NotNull
    @Override
    @Query("UPDATE users SET is_deleted = 1, deleted_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND is_deleted = 0")
    Mono<Void> deleteById(@NotNull Integer userId);

    @Transactional
    @Query("UPDATE users SET username = :#{#user.username}, email = :#{#user.email}, mobile_number = :#{#user.mobileNumber}, password_hash = :#{#user.passwordHash}, updated_at = CURRENT_TIMESTAMP WHERE user_id = :#{#user.userId} AND is_deleted = 0")
    Mono<Void> updateUser(@NotNull User user);
}
