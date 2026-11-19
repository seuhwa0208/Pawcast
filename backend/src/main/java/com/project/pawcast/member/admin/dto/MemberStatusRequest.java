package com.project.pawcast.member.admin.dto;

import com.project.pawcast.member.entity.Members;

public record MemberStatusRequest(
        Members.Status status
) {
}
