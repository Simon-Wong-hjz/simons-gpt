package com.simwong.simonsgpt.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
