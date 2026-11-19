package com.project.pawcast.auth.dto;

import com.project.pawcast.member.entity.Members;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String userId,
        String name,
        Members.Role role
) {
}
