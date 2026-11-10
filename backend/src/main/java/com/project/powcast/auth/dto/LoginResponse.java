package com.project.powcast.auth.dto;

public record LoginResponse(
    String loginId,
    String username,
    String accessToken
) {
}
