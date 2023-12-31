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
    @Column("user_id")
    private Integer userId;
    @Column("username")
    private String username;
    @Column("email")
    private String email;
    @Column("mobile_number")
    private String mobileNumber;
    @Column("password_hash")
    private String password;
    @Column("is_deleted")
    private Boolean isDeleted;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("deleted_at")
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
