package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.api.AssistantsApi;
import com.simwong.simonsgpt.model.Assistant;
import com.simwong.simonsgpt.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AssistantsController implements AssistantsApi {

    private final AssistantService assistantService;

    @Override
    public Mono<ResponseEntity<Flux<Assistant>>> assistantsGet(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(assistantService.listAssistants()));
    }
}
