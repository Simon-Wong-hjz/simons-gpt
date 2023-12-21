package com.simwong.simonsgpt.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Conversation {
    private Long conversationId;
    private Long userId; // Replace the User object with userId
    private String conversationData;
    private LocalDateTime timestamp;
}
