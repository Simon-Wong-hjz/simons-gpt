package com.simwong.simonsgpt.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Integer userId;
    private String username;
    private String email;
    private String mobileNumber;
    @Column("password_hash")
    private String password;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public UserDTO toDTO() {
        return UserDTO.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .mobileNumber(mobileNumber)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
