package com.project.pawcast.outhaccount.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.outhaccount.dto.SocialAccountLinkRequest;
import com.project.pawcast.outhaccount.dto.SocialLoginResponse;
import com.project.pawcast.outhaccount.dto.SocialSignupRequest;
import com.project.pawcast.outhaccount.entity.OauthAccount;
import com.project.pawcast.outhaccount.repository.OauthAccountRepository;
import com.project.pawcast.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private final GoogleAuthService googleAuthService;
    private final OauthAccountRepository oauthAccountRepository;
    private final MembersRepository membersRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // Google ID Token 검증 후 기존 계정 로그인 또는 후속 단계("LINK" / "SIGNUP") 반환
    @Transactional
    public SocialLoginResponse googleLogin(String idToken) {
        GoogleIdToken.Payload payload = verifyGooglePayload(idToken);
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        OauthAccount oauthAccount = findOauthAccount(providerUserId);
        if (oauthAccount != null) {
            return loginResponse(oauthAccount.getMember(), false);
        }

        // 동일 이메일로 가입된 기존 계정이 있으면 계정 연동 유도, 없으면 신규 가입 단계로 안내
        String nextStep = membersRepository.existsByEmail(email) ? "LINK" : "SIGNUP";
        return pendingResponse(nextStep, email, name);
    }

    // 추가 입력 정보 및 약관 동의 검증 후 Google 소셜 회원가입 및 로그인 처리
    @Transactional
    public SocialLoginResponse completeGoogleSignup(SocialSignupRequest request) {
        GoogleIdToken.Payload payload = verifyGooglePayload(request.idToken());
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = request.name().trim();

        OauthAccount existing = findOauthAccount(providerUserId);
        if (existing != null) {
            return loginResponse(existing.getMember(), false);
        }

        if (membersRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_MEMBER);
        }

        validateRequiredAgreements(request);

        Members member = createSocialMember(email, name, request.tel(), providerUserId, request);
        saveOauthAccount(member, providerUserId);
        return loginResponse(member, true);
    }

    // 기존 일반 회원 계정 비밀번호 확인 후 Google 소셜 계정 연동 및 로그인 처리
    @Transactional
    public SocialLoginResponse linkGoogleAccount(SocialAccountLinkRequest request) {
        GoogleIdToken.Payload payload = verifyGooglePayload(request.idToken());
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();

        OauthAccount existing = findOauthAccount(providerUserId);
        if (existing != null) {
            return loginResponse(existing.getMember(), false);
        }

        Members member = membersRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        if (member.getPasswordHash() == null || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        saveOauthAccount(member, providerUserId);
        return loginResponse(member, false);
    }

    // Google ID Token 검증 및 이메일 인증 여부 확인
    private GoogleIdToken.Payload verifyGooglePayload(String idToken) {
        GoogleIdToken.Payload payload = googleAuthService.verify(idToken);
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return payload;
    }

    // Google 소셜 계정 조회
    private OauthAccount findOauthAccount(String providerUserId) {
        return oauthAccountRepository
                .findByProviderAndProviderUserId(PROVIDER_GOOGLE, providerUserId)
                .orElse(null);
    }

    // 필수 약관 동의 여부 검사
    private void validateRequiredAgreements(SocialSignupRequest request) {
        if (!request.agreedTerms()) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);
        }
        if (!request.agreedPrivacy()) {
            throw new BusinessException(ErrorCode.REQUIRED_PRIVACY_AGREEMENT);
        }
        if (!request.agreedEfinance()) {
            throw new BusinessException(ErrorCode.REQUIRED_EFINANCE_AGREEMENT);
        }
    }

    // JWT 발급 및 로그인 완료 응답 생성
    private SocialLoginResponse loginResponse(Members member, boolean isNewMember) {
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().toString()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getId(),
                member.getEmail(),
                member.getRole().toString()
        );

        return new SocialLoginResponse(
                accessToken,
                refreshToken,
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getRole(),
                isNewMember,
                "LOGIN",
                member.getEmail()
        );
    }

    // 가입 또는 계정 연동 후속 단계 응답 생성
    private SocialLoginResponse pendingResponse(String nextStep, String email, String name) {
        return new SocialLoginResponse(
                null,
                null,
                null,
                null,
                name,
                null,
                false,
                nextStep,
                email
        );
    }

    // OauthAccount 매핑 엔티티 저장
    private void saveOauthAccount(Members member, String providerUserId) {
        oauthAccountRepository.save(OauthAccount.builder()
                .member(member)
                .provider(PROVIDER_GOOGLE)
                .providerUserId(providerUserId)
                .build());
    }

    // 소셜 가입 회원 엔티티 생성 및 저장
    private Members createSocialMember(
            String email,
            String name,
            String tel,
            String providerUserId,
            SocialSignupRequest request
    ) {
        Members member = Members.builder()
                .email(email)
                .name(name)
                .userId("google_" + providerUserId)
                .tel(tel)
                .role(Members.Role.USER)
                .status(Members.Status.ACTIVE)
                .isAgreedTerms(request.agreedTerms())
                .isAgreedPrivacy(request.agreedPrivacy())
                .isAgreedEfinance(request.agreedEfinance())
                .isAgreedMarketing(request.agreedMarketing())
                .build();

        return membersRepository.save(member);
    }
}