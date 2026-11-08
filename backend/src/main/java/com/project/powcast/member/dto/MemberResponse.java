package com.project.powcast.member.dto;

import com.project.powcast.member.entity.MemberRole;
import com.project.powcast.member.entity.MemberStatus;
import com.project.powcast.member.entity.Members;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String userId,
        String username,
        String email,
        String tel,
        MemberRole role,
        MemberStatus memberStatus,
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
                member.getUsername(),
                member.getEmail(),
                member.getTel(),
                member.getRole(),
                member.getMemberStatus(),
                member.isAgreedTerms(),
                member.isAgreedEfinance(),
                member.isAgreedPrivacy(),
                member.isAgreedMarketing(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
