package com.simwong.simonsgpt.controller;

import com.simwong.simonsgpt.domain.AssistantsResponse;
import com.simwong.simonsgpt.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AssistantsController {

    private final AssistantService assistantService;

    @GetMapping("/assistants")
    public Mono<AssistantsResponse> listAssistants() {
        return assistantService.listAssistants();
    }
}
