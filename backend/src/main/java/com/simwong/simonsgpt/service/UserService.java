package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.domain.User;
import com.simwong.simonsgpt.domain.UserDto;
import com.simwong.simonsgpt.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> registerUser(UserDto userDto) {
        User newUser = new User();
        newUser.setUsername(userDto.getUsername());
        newUser.setEmail(userDto.getEmail());
        newUser.setPhoneNumber(userDto.getPhoneNumber());
        newUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        newUser.setCreatedAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    // Method for user login and other necessary services
    // login
    public Mono<User> loginUser(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> verifyPassword(password, user.getPasswordHash()));
    }

    private boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
