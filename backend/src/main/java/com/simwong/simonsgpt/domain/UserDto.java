package com.simwong.simonsgpt.domain;

import lombok.Data;

@Data
public class UserDto {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
}
