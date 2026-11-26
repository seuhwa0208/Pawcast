package com.project.pawcast.member.service;

import com.project.pawcast.auth.dto.ChangePasswordRequest;
import com.project.pawcast.auth.repository.RefreshTokenRepository;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.dto.MemberRequest;
import com.project.pawcast.member.dto.MemberResponse;
import com.project.pawcast.member.dto.PasswordRequest;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MembersRepository membersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    // 기본 회원 조회
    public Members findById(Long id) {
        return membersRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
    }

    // 비밀번호 변경: 본인 확인 -> 새 비밀번호 검증 -> 암호화 및 즉시 반영 -> 기존 토큰 폐기
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Members member = findById(memberId);

        boolean currentMatches = passwordEncoder.matches(request.password(), member.getPasswordHash());
        boolean sameAsNew = passwordEncoder.matches(request.newPassword(), member.getPasswordHash());
        log.info(
                "changePassword 시도: memberId={}, userId={}, currentMatches={}, sameAsNew={}",
                member.getId(),
                member.getUserId(),
                currentMatches,
                sameAsNew
        );

        // 현재 입력한 비밀번호가 기존 비밀번호와 일치하는지 확인
        if (!currentMatches) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 새 비밀번호 필수 및 확인 값 일치 여부 검증
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_REQUIRED);
        }
        if (!request.newPassword().equals(request.newPasswordCheck())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_MISMATCH);
        }

        // 기존 비밀번호와 동일한 비밀번호로 변경하는지 확인
        if (sameAsNew) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD);
        }

        // 새 비밀번호 해시를 저장하고 flush한 뒤, 벌크 쿼리로 기존 RefreshToken을 폐기
        String encodedPassword = passwordEncoder.encode(request.newPassword());
        member.setPasswordHash(encodedPassword);
        membersRepository.saveAndFlush(member);

        int revokedCount = refreshTokenRepository.revokeAllByMember(memberId);
        log.info(
                "changePassword 완료: memberId={}, revokedTokens={}, updatedAt={}",
                member.getId(),
                revokedCount,
                member.getUpdatedAt()
        );
    }

    // 내 정보 조회
    public MemberResponse findByMyInfo(Long id) {
        return MemberResponse.from(findById(id));
    }

    // 내 정보 수정
    @Transactional
    public MemberResponse updateMyInfo(Long id, MemberRequest request) {
        Members member = findById(id);
        member.setName(request.name());
        member.setTel(request.tel());
        member.setAgreedMarketing(request.isAgreedMarketing());

        return MemberResponse.from(membersRepository.saveAndFlush(member));
    }

    // 내 정보 수정 전 비밀번호 확인
    public boolean checkPasswordViewMyInfo(Long id, PasswordRequest request) {
        return passwordEncoder.matches(request.password(), findById(id).getPasswordHash());
    }

    // 회원 탈퇴
    @Transactional
    public void deletedMyInfo(Long id) {
        Members member = membersRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (member.getStatus() == Members.Status.DELETED) {
            return;
        }
        member.setStatus(Members.Status.DELETED);
        member.setDeletedAt(LocalDateTime.now());
        membersRepository.saveAndFlush(member);
        refreshTokenRepository.revokeAllByMember(id);
    }
}
