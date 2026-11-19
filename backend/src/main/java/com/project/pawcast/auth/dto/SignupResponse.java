package com.project.pawcast.auth.dto;

public record SignupResponse(
        String name,
        String userId,
        String email
) {
}
