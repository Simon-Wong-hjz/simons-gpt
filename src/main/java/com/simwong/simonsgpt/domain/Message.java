package com.simwong.simonsgpt.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("messages")
public class Message {
    @Id
    private Integer messageId;
    private Integer conversationId;
    private String role;
    private String content;
    private Byte isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Boolean getIsDeleted() {
        return isDeleted != null && isDeleted == 1;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted ? (byte) 1 : (byte) 0;
    }
}
