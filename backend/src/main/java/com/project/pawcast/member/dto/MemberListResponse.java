package com.project.pawcast.member.dto;

import com.project.pawcast.member.entity.Members;

import java.time.LocalDateTime;

public record MemberListResponse(
        Long id,
        String userId,
        String name,
        String email,
        String tel,
        Members.Role role,
        Members.Status status,
        boolean agreedTerms,
        boolean agreedEfinance,
        boolean agreedPrivacy,
        boolean agreedMarketing,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long dogCount,
        Long catCount
) {
    public static MemberListResponse from(Members member, Long dogCount, Long catCount) {
        return new MemberListResponse(
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getEmail(),
                member.getTel(),
                member.getRole(),
                member.getStatus(),
                member.isAgreedTerms(),
                member.isAgreedEfinance(),
                member.isAgreedPrivacy(),
                member.isAgreedMarketing(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                dogCount,
                catCount
        );
    }
}
