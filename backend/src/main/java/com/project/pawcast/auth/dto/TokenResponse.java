package com.project.pawcast.auth.dto;

import com.project.pawcast.member.entity.Members;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Members.Role role
) {
}
