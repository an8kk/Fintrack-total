package com.fintrack.backend.dto;

import lombok.Data;

@Data
public class UserUpdateDto {
    private String username;
    private String email;
}