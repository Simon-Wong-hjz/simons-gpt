package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.domain.AssistantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final OpenAIClient openAIClient;

    public Mono<AssistantsResponse> listAssistants() {
        return openAIClient.listAssistants();
    }

}
