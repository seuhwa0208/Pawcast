package com.project.pawcast.outhaccount.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.outhaccount.dto.SocialLoginResponse;
import com.project.pawcast.outhaccount.dto.SocialAccountLinkRequest;
import com.project.pawcast.outhaccount.dto.SocialSignupRequest;
import com.project.pawcast.outhaccount.entity.OauthAccount;
import com.project.pawcast.outhaccount.repository.OauthAccountRepository;
import com.project.pawcast.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginService {

    private final GoogleAuthService googleAuthService;
    private final OauthAccountRepository oauthAccountRepository;
    private final MembersRepository membersRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 구글 소셜 로그인
    @Transactional
    public SocialLoginResponse googleLogin(String idToken) {    // 프론트가 준 토큰이겠지?

        // Google ID Token 검증
        GoogleIdToken.Payload payload = googleAuthService.verify(idToken);

        // Google 사용자 정보 추출
        String provider = "GOOGLE";
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // 이메일 인증 여부 확인 - 구글 이메일 인증임
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {throw new BusinessException(ErrorCode.FORBIDDEN);}

        // 기존 소셜 계정 조회 있는지 확인
        OauthAccount oauthAccount = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).orElse(null);

        if (oauthAccount != null) return loginResponse(oauthAccount.getMember(), false);
        // 신규 가입 또는 기존 일반 계정 연결은 모두 Google ID Token을 다시 검증하는 후속 단계에서 완료합니다.
        return pendingResponse(membersRepository.existsByEmail(email) ? "LINK" : "SIGNUP", email, name);
    }

    @Transactional
    public SocialLoginResponse completeGoogleSignup(SocialSignupRequest request) {
        GoogleIdToken.Payload payload = googleAuthService.verify(request.idToken());
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        String name = request.name().trim();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) throw new BusinessException(ErrorCode.FORBIDDEN);
        OauthAccount existing = oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", providerUserId).orElse(null);
        if (existing != null) return loginResponse(existing.getMember(), false);
        if (membersRepository.existsByEmail(email)) throw new BusinessException(ErrorCode.DUPLICATE_MEMBER);
        if (!request.agreedTerms()) throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);
        if (!request.agreedPrivacy()) throw new BusinessException(ErrorCode.REQUIRED_PRIVACY_AGREEMENT);
        if (!request.agreedEfinance()) throw new BusinessException(ErrorCode.REQUIRED_EFINANCE_AGREEMENT);
        Members member = createSocialMember(email, name, request.tel(), providerUserId, request);
        saveOauthAccount(member, providerUserId);
        return loginResponse(member, true);
    }

    @Transactional
    public SocialLoginResponse linkGoogleAccount(SocialAccountLinkRequest request) {
        GoogleIdToken.Payload payload = googleAuthService.verify(request.idToken());
        String providerUserId = payload.getSubject();
        String email = payload.getEmail();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) throw new BusinessException(ErrorCode.FORBIDDEN);
        OauthAccount existing = oauthAccountRepository.findByProviderAndProviderUserId("GOOGLE", providerUserId).orElse(null);
        if (existing != null) return loginResponse(existing.getMember(), false);
        Members member = membersRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (member.getPasswordHash() == null || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        saveOauthAccount(member, providerUserId);
        return loginResponse(member, false);
    }

    private SocialLoginResponse loginResponse(Members member, boolean isNewMember) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(),member.getEmail(),member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(),member.getEmail(),member.getRole().toString());
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

    private SocialLoginResponse pendingResponse(String nextStep, String email, String name) {
        return new SocialLoginResponse(null, null, null, null, name, null, false, nextStep, email);
    }

    private void saveOauthAccount(Members member, String providerUserId) {
        oauthAccountRepository.save(OauthAccount.builder()
                .member(member)
                .provider("GOOGLE")
                .providerUserId(providerUserId)
                .build());
    }

    // 회원가입
    @Transactional
    private Members createSocialMember(String email, String name, String tel, String providerUserId, SocialSignupRequest request) {
        // Members 엔티티의 필수 필드에 맞춰 회원 생성
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
