package com.simwong.simonsgpt.service;

import com.simwong.simonsgpt.client.OpenAIClient;
import com.simwong.simonsgpt.model.Assistant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private final OpenAIClient openAIClient;

    public Flux<Assistant> listAssistants() {
        return openAIClient.listAssistants();
    }

}
