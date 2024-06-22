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
@Table("conversations")
public class Conversation {
    @Id
    private Integer conversationId;
    private Integer userId;
    private String title;
    private Byte isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Boolean getIsDeleted() {
        return isDeleted != null && isDeleted == 1;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted ? (byte) 1 : (byte) 0;
    }
}
