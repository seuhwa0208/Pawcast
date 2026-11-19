package com.project.powcast.outhaccount.dto;

public record SocialLoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewMember
) {
}
