package com.project.powcast.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String passwordHash;
}
