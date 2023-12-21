package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.domain.UserDto;
import com.simwong.simonsgpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public Mono<ResponseEntity<Object>> registerUser(@RequestBody UserDto userDto) {
        return userService.registerUser(userDto)
                .map(savedUser -> ResponseEntity.accepted().build())
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Object>> loginUser(@RequestBody UserDto userDto) {
        return userService.loginUser(userDto.getUsername(), userDto.getPassword())
                .map(savedUser -> ResponseEntity.accepted().build())
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }
}
