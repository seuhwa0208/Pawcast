package com.project.powcast.auth.dto;

public record SignupResponse(
        String name,
        String userId,
        String email
) {
}
