package com.fintrack.backend.dto;

import lombok.Data;

@Data
public class AdminUpdateUserDto {
    private String username;
    private String email;
    private String password;
    private String role;
    private Boolean blocked;
}
