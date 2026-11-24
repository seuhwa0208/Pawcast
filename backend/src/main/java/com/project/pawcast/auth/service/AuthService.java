package com.project.pawcast.auth.service;

import com.project.pawcast.auth.dto.*;
import com.project.pawcast.auth.entity.EmailVerification;
import com.project.pawcast.auth.entity.RefreshTokens;
import com.project.pawcast.auth.repository.EmailVerificationRepository;
import com.project.pawcast.auth.repository.RefreshTokenRepository;
import com.project.pawcast.common.email.EmailService;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.EmailSendException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.common.util.PasswordGenerator;
import com.project.pawcast.member.dto.MemberResponse;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final MembersRepository membersRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // 아이디 중복 검사
    public boolean checkUserId(String userId) {
        return membersRepository.existsByUserId(userId);
    }

    public boolean checkEmail(EmailRequest email) {
        return membersRepository.existsByEmail(email.email());
    }

    public boolean checkEmail(String email) {
        return membersRepository.existsByEmail(email);
    }

    // 이메일로 회원 정보 불러오기
    public Members findByEmailMember(String email) {
        return membersRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
    }

    // 이메일로 이메일 검증 정보 확인
    public EmailVerification checkVerificationEmail(String email) {
        return emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new EmailSendException(ErrorCode.NOT_FOUND_EMAIL_VERIFICATION));
    }

    // 이메일 인증 검증 - 이메일로 보낸 코드와 저장된 코드 비교
    @Transactional
    public boolean checkVerifyEmail(String email, String checkCode) {
        EmailVerification emailVerification = checkVerificationEmail(email);

        // 인증 완료된 이메일인지 확인
        if (emailVerification.getVerifiedAt() != null) {
            throw new BusinessException(ErrorCode.VERIFICATION_COMPLETED_EMAIL);
        }

        // 이메일 코드 날짜 만료 확인
        if (!LocalDateTime.now().isBefore(emailVerification.getExpiresAt())) {
            throw new BusinessException(ErrorCode.EXPIRED_EMAIL_CODE);
        }

        // 이메일 코드 체크 - 일치하지 않으면 - false 반환
        if (!passwordEncoder.matches(checkCode, emailVerification.getVerificationCode())) {
            return false;
        }

        // 성공하면 인증 날짜 저장하고 true 반환
        emailVerification.setVerifiedAt(LocalDateTime.now());
        return true;
    }

    // 회원가입
    @Transactional
    public MemberResponse register(SignUpRequest request) {
        Members member = new Members();

        // 아이디 중복 검사
        if (checkUserId(request.userId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERID);
        }

        // 이메일 검증이 유효한지
        EmailVerification emailVerification = checkVerificationEmail(request.email());
        if (emailVerification.getVerifiedAt() == null) {
            throw new BusinessException(ErrorCode.NOT_VERIFIED_EMAIL);
        }
        if (membersRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 일치 확인
        if (!request.passwordHash().equals(request.checkPasswordHash())) {
            throw new BusinessException(ErrorCode.CHECK_PASSWORD_ERROR);
        }

        // 약관 검사
        if (!request.isAgreedTerms()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);
        }
        if (!request.isAgreedPrivacy()) {
            throw new BusinessException(ErrorCode.REQUIRED_PRIVACY_AGREEMENT);
        }
        if (!request.isAgreedEfinance()) {
            throw new BusinessException(ErrorCode.REQUIRED_EFINANCE_AGREEMENT);
        }

        member.setUserId(request.userId());
        member.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        member.setName(request.name());
        member.setEmail(request.email());
        member.setTel(request.tel());
        member.setRole(Members.Role.USER);
        member.setStatus(Members.Status.ACTIVE);

        member.setAgreedTerms(request.isAgreedTerms());
        member.setAgreedPrivacy(request.isAgreedPrivacy());
        member.setAgreedEfinance(request.isAgreedEfinance());
        member.setAgreedMarketing(request.isAgreedMarketing());

        return MemberResponse.from(membersRepository.save(member));
    }

    // 일반 로그인 인증
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Members member = membersRepository.findByUserId(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (member.getStatus() == Members.Status.INACTIVE) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE);
        }
        if (member.getStatus() == Members.Status.DELETED) {
            throw new BusinessException(ErrorCode.MEMBER_DELETED);
        }
        if (!passwordEncoder.matches(request.passwordHash(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole().name());
        String newRefreshToken = tokenProvider.createRefreshToken(member.getId(), member.getEmail(), member.getRole().name());

        refreshTokenRepository.revokeAllByMember(member.getId());
        RefreshTokens refreshTokens = RefreshTokens.builder()
                .member(member)
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.refreshTokenValiditySeconds()))
                .revoked(false)
                .userAgent(header(httpRequest, "User-Agent"))
                .ipAddress(clientIp(httpRequest))
                .build();
        refreshTokenRepository.save(refreshTokens);

        return new LoginResponse(accessToken, newRefreshToken, member.getId(), member.getUserId(), member.getName(), member.getRole());
    }

    // 로그아웃
    @Transactional
    public void logout(String refreshTokenValue) {
        try {
            var claims = tokenProvider.parse(refreshTokenValue);
            if (!tokenProvider.isRefreshToken(claims)) {
                return;
            }
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .filter(RefreshTokens::isUsable)
                    .ifPresent(RefreshTokens::revoke);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.TOKEN_EXPIRED && e.getErrorCode() != ErrorCode.TOKEN_INVALID) {
                throw e;
            }
        }
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(String email) {
        String tempPassword = tempPasswordCreate();
        Members member = findByEmailMember(email);
        emailService.sendEmail(email, "임시 비밀번호 발급", "임시 비밀버호는 : " + tempPassword + "입니다.");
        member.setPasswordHash(passwordEncoder.encode(tempPassword));
    }

    private String tempPasswordCreate() {
        return PasswordGenerator.createTempPassword();
    }

    // 토큰 재발급
    @Transactional
    public TokenResponse refresh(String refreshTokenValue, HttpServletRequest httpRequest) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        var claims = tokenProvider.parse(refreshTokenValue);
        if (!tokenProvider.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        RefreshTokens stored = refreshTokenRepository.findByTokenForUpdate(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        if (!stored.isUsable()) {
            log.warn("폐기된 refresh 토큰 재사용 시도: memberId={}", stored.getMember().getId());
            // 늦게 도착한 중복 refresh 요청이 새로 회전된 세션까지 무효화하지 않게 합니다.
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Members member = stored.getMember();
        if (member.getStatus() == Members.Status.DELETED) {
            throw new BusinessException(ErrorCode.MEMBER_DELETED);
        }

        stored.revoke();
        String accessToken = tokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole().name());
        String newRefresh = tokenProvider.createRefreshToken(member.getId(), member.getEmail(), member.getRole().name());
        refreshTokenRepository.save(RefreshTokens.builder()
                .member(member)
                .token(newRefresh)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.refreshTokenValiditySeconds()))
                .userAgent(header(httpRequest, "User-Agent"))
                .ipAddress(clientIp(httpRequest))
                .build()
        );
        return new TokenResponse(accessToken, newRefresh, member.getRole());
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

    @Transactional
    public boolean checkVerifyEmail(EmailRequest request, String checkCode) {
        return checkVerifyEmail(request.email(), checkCode);
    }
}

