package com.simwong.simonsgpt.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class HelloController {
    @GetMapping("/")
    public Mono<ResponseEntity<String>> hello() {
        return Mono.just(ResponseEntity.ok("Hello World"));
    }
}
