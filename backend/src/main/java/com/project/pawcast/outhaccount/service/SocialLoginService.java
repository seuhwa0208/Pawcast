package com.project.pawcast.outhaccount.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.outhaccount.dto.SocialLoginResponse;
import com.project.pawcast.outhaccount.entity.OauthAccount;
import com.project.pawcast.outhaccount.repository.OauthAccountRepository;
import com.project.pawcast.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginService {

    private final GoogleAuthService googleAuthService;
    private final OauthAccountRepository oauthAccountRepository;
    private final MembersRepository membersRepository;
    private final JwtTokenProvider jwtTokenProvider;

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

        Members member;
        boolean isNewMember = false;

        // 기존 계정이 없는 경우 자동 회원가입
        if (oauthAccount == null) { // 소셜계정으로 가입된 회원이 없으면
            // 기존 이메일 회원과 자동으로 연결하지 않음 - 에러남 - 계정연동 기능이 따로 있어야함 - 후순위로 밈
            if (membersRepository.existsByEmail(email)) {throw new BusinessException(ErrorCode.DUPLICATE_MEMBER);}

            // 회원 자동 생성
            member = createSocialMember(email, name);

            // OAuth 계정 연결
            OauthAccount newAccount = OauthAccount.builder()
                    .member(member)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .build();

            oauthAccountRepository.save(newAccount);
            isNewMember = true;

        } else {
            // 기존 소셜 계정
            member = oauthAccount.getMember();
        }

        // JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(),member.getEmail(),member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(),member.getEmail(),member.getRole().toString());

        // 로그인 결과 반환 -> 자동 로그인?
        return new SocialLoginResponse(
                accessToken,
                refreshToken,
                member.getId(),
                member.getUserId(),
                member.getName(),
                member.getRole(),
                isNewMember
        );
    }

    // 회원가입
    @Transactional
    private Members createSocialMember(String email, String name) {
        // Members 엔티티의 필수 필드에 맞춰 회원 생성
        Members member = Members.builder()
                .email(email)
                .name(name)
                .role(Members.Role.USER)
                .status(Members.Status.ACTIVE)
                .isAgreedTerms(true)
                .isAgreedPrivacy(true)
                .isAgreedEfinance(true)
                .isAgreedMarketing(false)
                .build();

        return membersRepository.save(member);
    }
}
