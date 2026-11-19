package com.project.pawcast.outhaccount.dto;

public record SocialLoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewMember
) {
}
