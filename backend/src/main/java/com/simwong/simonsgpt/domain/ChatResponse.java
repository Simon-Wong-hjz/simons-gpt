package com.simwong.simonsgpt.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatResponse {
    @JsonProperty("id")
    private String id;
    @JsonProperty("object")
    private String object;
    @JsonProperty("created")
    private Integer created;
    @JsonProperty("model")
    private String model;
    @JsonProperty("choices")
    private List<Choices> choices;
    @JsonProperty("usage")
    private Usage usage;
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    @Builder
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Builder
    @Data
    public static class Choices {
        @JsonProperty("index")
        private Integer index;
        @JsonProperty("message")
        private Message message;
        @JsonProperty("logprobs")
        private Object logprobs;
        @JsonProperty("finish_reason")
        private String finishReason;

        @Builder
        @Data
        public static class Message {
            @JsonProperty("role")
            private String role;
            @JsonProperty("content")
            private String content;
        }
    }
}
