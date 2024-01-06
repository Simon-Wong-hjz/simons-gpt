package com.simwong.simonsgpt.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer userId;
    private String username;
    private String email;
    private String mobileNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User toUser() {
        return User.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .mobileNumber(mobileNumber)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}