package com.project.powcast.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String userId,

        @NotBlank
        String passwordHash
        ) {}
