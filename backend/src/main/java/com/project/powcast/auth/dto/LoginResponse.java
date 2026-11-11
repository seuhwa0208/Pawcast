package com.project.powcast.auth.dto;

import com.project.powcast.member.entity.MemberRole;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String userId,
        String name,
        MemberRole role
) {
}
