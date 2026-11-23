package com.project.pawcast.outhaccount.dto;

import com.project.pawcast.member.entity.Members;

public record SocialLoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String userId,
        String name,
        Members.Role role,
        boolean isNewMember,
        String nextStep,
        String email
) {
}