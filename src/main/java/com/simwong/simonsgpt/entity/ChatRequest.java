package com.simwong.simonsgpt.entity;

import com.simwong.simonsgpt.domain.Message;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Integer conversationId;
    @NotNull
    private List<Message> chatMessages;
}
