package com.project.pawcast.security;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import org.springframework.stereotype.Component;

// 권한 검사 어드민, 매니저
@Component
public class AuthorizationValidator {
    // 관리자 인지
    public void validateAdmin(Members member) {
        if (member.getRole() != Members.Role.ADMIN) {throw new BusinessException(ErrorCode.FORBIDDEN);}
    }

    // 관리자 매니저 인지
    public void validateManager(Members member) {
        if (member.getRole() != Members.Role.ADMIN && member.getRole() != Members.Role.MANAGER) {throw new BusinessException(ErrorCode.FORBIDDEN);}
    }
}
