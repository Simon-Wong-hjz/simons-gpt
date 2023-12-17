package com.simwong.simonsgpt.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatRequest {
    @JsonProperty("model")
    private String model;
    @JsonProperty("messages")
    private List<Messages> messages;
    @JsonProperty("stream")
    private Boolean stream;

    @Builder
    @Data
    public static class Messages {
        @JsonProperty("role")
        private String role;
        @JsonProperty("content")
        private String content;
    }
}
