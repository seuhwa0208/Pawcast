package com.project.powcast.member.admin.dto;

import com.project.powcast.member.entity.Members;

public record MemberStatusRequest(
        Members.Status status
) {
}
