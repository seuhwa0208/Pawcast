package com.project.powcast.member.service;

import com.project.powcast.auth.dto.ChangePasswordRequest;
import com.project.powcast.auth.repository.RefreshTokenRepository;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.dto.MemberRequest;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.member.dto.PasswordRequest;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    // 기본 findById
    public Members findById(Long id){
        return memberRepository.findById(id).orElseThrow(()-> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    //비밀번호 재설정
    // 재설정 하고 나면 로그아웃 및 재 로그인
    // 본인 확인 - 변경 - 저장 - 로그아웃 - 재로그인
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request){
        // 회원 조회
        Members member = findById(memberId);

        // 현재 입력한 비밀번호가 현재 비밀번호와 일치하는지 확인
        if(!passwordEncoder.matches(request.password(), (member.getPasswordHash()))) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 새 비밀번호 입력
        if(request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_REQUIRED);
        }

        // 비밀번호 확인과 일치하는지 확인
        if(!request.newPassword().equals(request.newPasswordCheck())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_MISMATCH);
        }

        // 기존 비밀번호와 새 비밀번호가 같은지 확인
        if (passwordEncoder.matches(request.newPassword(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }
        // DB 에 적용
        member.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // 비밀번호 변경했으니 토큰 폐기
        refreshTokenRepository.revokeAllByMember(memberId);
    }

    // 내 정보 조회
    public MemberResponse findByMyInfo(Long id){
        return MemberResponse.from(findById(id));
    }

    // 내 정보 수정
    @Transactional
    public MemberResponse updateMyInfo(Long id, MemberRequest request){
        Members member = findById(id);
        member.setName(request.name());
        member.setTel(request.tel());
        member.setAgreedMarketing(request.isAgreedMarketing());

        return MemberResponse.from(memberRepository.save(member));
    }

    // 내 정보 수정전 비밀번호 확인
    public boolean checkPasswordViewMyInfo(Long id, PasswordRequest request){
        return passwordEncoder.matches(request.password(), findById(id).getPasswordHash());
    }

    // 회원 탈퇴
    @Transactional
    public void deletedMyInfo(Long id){
        Members member = findById(id);
        member.setStatus(Members.Status.DELETED);
        member.setDeletedAt(LocalDateTime.now());
    }
}
