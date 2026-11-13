package com.project.powcast.auth.dto;

import com.project.powcast.member.entity.Members;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String userId,
        String name,
        Members.Role role
) {
}
