package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.api.UserApi;
import com.simwong.simonsgpt.domain.User;
import com.simwong.simonsgpt.domain.UserDTO;
import com.simwong.simonsgpt.service.JwtTokenService;
import com.simwong.simonsgpt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    @Override
    public Mono<ResponseEntity<UserDTO>> registerUser(User user, ServerWebExchange exchange) {
        return userService.registerUser(user)
                .map(savedUserDTO -> ResponseEntity.ok().body(savedUserDTO));
    }

    @Override
    public Mono<ResponseEntity<String>> loginUser(User user, ServerWebExchange exchange) {
        return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
                )
                .flatMap(authentication -> userService.findByUsername(authentication.getName()))
                .map(userDetail -> ResponseEntity.ok(jwtTokenService.generateToken(userDetail)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
}
