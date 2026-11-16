package com.project.powcast.security;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import org.springframework.stereotype.Component;

// 권한 검사 어드민, 매니저
@Component
public class AuthorizationValidator {
    public void validateAdmin(Members member) {
        if (member.getRole() != Members.Role.ADMIN) {throw new BusinessException(ErrorCode.FORBIDDEN);}
    }

    public void validateManager(Members member) {
        if (member.getRole() != Members.Role.ADMIN && member.getRole() != Members.Role.MANAGER) {throw new BusinessException(ErrorCode.FORBIDDEN);}
    }
}
