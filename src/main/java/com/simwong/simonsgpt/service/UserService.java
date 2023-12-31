package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.domain.CustomUserDetails;
import com.simwong.simonsgpt.domain.User;
import com.simwong.simonsgpt.domain.UserDTO;
import com.simwong.simonsgpt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<UserDTO> registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user)
                .map(User::toDTO)
                .doOnError(throwable -> {
                    if (throwable instanceof DuplicateKeyException) {
                        throw new RuntimeException("用户已存在");
                    }
                    throw new RuntimeException("未知错误");
                });
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(CustomUserDetails::new);
    }
}
