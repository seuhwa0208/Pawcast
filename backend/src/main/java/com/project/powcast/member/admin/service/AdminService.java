package com.project.powcast.member.admin.service;


import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.admin.dto.MemberRoleRequest;
import com.project.powcast.member.admin.dto.MemberRoleResponse;
import com.project.powcast.member.admin.dto.MemberStatusRequest;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MembersRepository;
import com.project.powcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final AuthorizationValidator validator;
    private final MembersRepository membersRepository;


    //회원 전체 목록
    public List<MemberResponse> getMemberList(Long memberId) {
        // 권한검사
        Members member = membersRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateAdmin(member);
        // 전체 목록
        return membersRepository.findAllByStatusNotOrderByCreatedAtAsc(Members.Status.DELETED).stream().map(MemberResponse::from).toList();
    }
    
    // 회원 역할 변경
    @Transactional
    public void changeMemberRole(Long adminId, Long userId, MemberRoleRequest request){
        // 권한 검사
        Members member = membersRepository.findById(adminId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateAdmin(member);
        Members user = membersRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        user.setRole(request.role());
    }
    
    // 회원 상태 변경
    @Transactional
    public void changeMemberStatus(Long adminId, Long userId, MemberStatusRequest request){
        Members member = membersRepository.findById(adminId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateAdmin(member);
        Members user = membersRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        user.setStatus(request.status());
    }
}
