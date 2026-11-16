package com.project.powcast.member.dto;

import com.project.powcast.member.entity.Members;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String userId,
        String name,
        String email,
        String tel,
        Members.Role role,
        Members.Status status,
        boolean isAgreedTerms,
        boolean isAgreedEfinance,
        boolean isAgreedPrivacy,
        boolean isAgreedMarketing,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
)   {
    public static MemberResponse from(Members member) {
        return new MemberResponse(
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
                member.getUpdatedAt()
        );
    }
}
