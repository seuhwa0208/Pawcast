package com.project.powcast.member.admin.dto;

import com.project.powcast.member.entity.Members;

public record MemberRoleRequest(
        Members.Role role
) {
}
