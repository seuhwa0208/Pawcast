package com.project.powcast.auth.service;

import com.project.powcast.auth.dto.*;
import com.project.powcast.auth.entity.RefreshTokens;
import com.project.powcast.auth.repository.RefreshTokenRepository;
import com.project.powcast.common.email.EmailService;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.EmailSendException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.common.util.PasswordGenerator;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.auth.entity.EmailVerification;
import com.project.powcast.auth.repository.EmailVerificationRepository;
import com.project.powcast.member.entity.MemberRole;
import com.project.powcast.member.entity.MemberStatus;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    //아이디 중복 검사
    public boolean checkUserId(String userId){
        return memberRepository.existsByUserId(userId);
    }
    
    // 이메일로 회원 정보 불러오기
    public Members findByEmailMember(String email){
        return memberRepository.findByEmail(email).orElseThrow(()-> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }


    // 이메일로 이메일 검증 정보 확인
    public EmailVerification checkVerificationEmail(String email){
        return emailVerificationRepository.findByEmail(email).orElseThrow(()-> new EmailSendException(ErrorCode.NOT_FOUND_EMAIL_VERIFICATION));
    }

    //이메일 인증 검증 - 이메일로 보낸 코드와 저장된 코드 비교
    //이메일 기반으로 코드가 일치하는지 비교
    //만족하면 인증된 시간 저장
    //인증만 하고 가입을 안하게되면? 다시 가입할때 이미 인증이 완료된 상태잖아? 그러면 안되니깐
    //이메일이 인증이 완료됬는데 이 이메일 기반으로 회원아이디가 없으면? 인증완료된 정보를 파기해야함 - 언제? - 다시 이메일 검증할때
    // 이메일 보내기전에 이 이메일로 가입된 회원정보가 있나 확인하고 있으면 막고, 없으면 인증정보 파기후 다시 생성 - 이메일인증서비스에서 파기하고 새로만듬
    // 여기선 코드 검사하고 성공하면 완료시간만 업데이트하면 될듯?
    @Transactional
    public boolean checkVerifyEmail(String email, String checkCode){
        EmailVerification emailVerification = checkVerificationEmail(email);

        // 인증 완료된 이메일인지 확인
        if(emailVerification.getVerifiedAt() != null) {throw new BusinessException(ErrorCode.VERIFICATION_COMPLETED_EMAIL);}

        // 이메일 코드 날짜 만료 확인
        if (!LocalDateTime.now().isBefore(emailVerification.getExpiresAt())) { throw new BusinessException(ErrorCode.EXPIRED_EMAIL_CODE);}

        // 이메일 코드 체크 - 일치하지 않으면 - false 반환
        if(!passwordEncoder.matches(checkCode,emailVerification.getVerificationCode())){return false;}

        // 성공하면 인증 날짜 저장하고 true 반환
        emailVerification.setVerifiedAt(LocalDateTime.now());
        return true;
    }


    //회원가입
    @Transactional
    public MemberResponse register(SignUpRequest request){
        Members member = new Members();

        // 아이디 중복 검사
        if (checkUserId(request.userId())){throw new BusinessException(ErrorCode.DUPLICATE_USERID);}

        // 이메일 검증이 유효한지
        // 적혀있는 이메일 인증 완료됫는지 검사 및 방금 전 이메일과 일치하는가?
        EmailVerification emailVerification = checkVerificationEmail(request.email());  // 이메일 검증 정보 확인(유무 확인)
        // 이메일이 인증 되었는지
        if(emailVerification.getVerifiedAt() == null){throw new BusinessException(ErrorCode.NOT_VERIFIED_EMAIL);}
        // 이미 가입된 이메일인지(중복검사)
        if (memberRepository.existsByEmail(request.email())){throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);}

        // 비밀번호 일치 확인
        if(!request.passwordHash().equals(request.checkPasswordHash())) {throw new BusinessException(ErrorCode.CHECK_PASSWORD_ERROR);}

        // 이용 약관 검사
        if(!request.isAgreedTerms()) {throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);}

        // 개인 정보 약관 검사
        if(!request.isAgreedPrivacy()) {throw new BusinessException(ErrorCode.REQUIRED_PRIVACY_AGREEMENT);}

        // 전자 금융 거래 약관 검사
        if(!request.isAgreedEfinance()) {throw new BusinessException(ErrorCode.REQUIRED_EFINANCE_AGREEMENT);}

        // 아이디 저장
        member.setUserId(request.userId());
        //비밀번호 암호화 및 저장
        member.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
        //이름 저장
        member.setName(request.name());
        //이메일 저장
        member.setEmail(request.email());
        //전화번호 저장
        member.setTel(request.tel());
        //ROLE 값의 기본값 지정
        member.setRole(MemberRole.USER);
        //상태 값의 기본값 지정
        member.setMemberStatus(MemberStatus.ACTIVE);

        member.setAgreedTerms(request.isAgreedTerms());
        member.setAgreedPrivacy(request.isAgreedPrivacy());
        member.setAgreedEfinance(request.isAgreedEfinance());
        member.setAgreedMarketing(request.isAgreedMarketing());
        //가입정보 리턴
        return MemberResponse.from(memberRepository.save(member));
    }


    // 일반 로그인 인증
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest){
        // 계정조회
        Members member = memberRepository.findByUserId(request.userId()).orElseThrow(()-> new BusinessException(ErrorCode.LOGIN_FAILED));
        // 비활성화 및 탈퇴한 회원 여부 조회
        if (member.getMemberStatus() == MemberStatus.INACTIVE){throw new BusinessException(ErrorCode.MEMBER_INACTIVE);}
        if (member.getMemberStatus() == MemberStatus.DELETED){throw new BusinessException(ErrorCode.MEMBER_DELETED);}
        // 비밀번호 확인
        if(!passwordEncoder.matches(request.passwordHash(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        //역할에 맞는 인증정보 발급
        String accessToken = tokenProvider.createAccessToken(member.getId(),member.getEmail(),member.getRole().name());
        String newRefreshToken = tokenProvider.createRefreshToken(member.getId(),member.getEmail(),member.getRole().name());
        

        //기존 토큰 조회 및 폐기
        refreshTokenRepository.revokeAllByMember(member.getId());
        //토큰 발급했으니 DB에 저장
        RefreshTokens refreshTokens = RefreshTokens.builder()
                .member(member)
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(tokenProvider.refreshTokenValiditySeconds()))
                .revoked(false)
                .userAgent(header(httpRequest,"User-Agent"))
                .ipAddress(clientIp(httpRequest))
                .build();
        refreshTokenRepository.save(refreshTokens);

        return new LoginResponse(accessToken,newRefreshToken,member.getId(),member.getUserId(),member.getName(),member.getRole());
    }

    // 로그아웃
    // 로그아웃시 토큰이 제거되어야하는데
    // 제거 실패했다고 로그아웃이 안되면 안되니깐
    // 시도는 유효성, 검증, DB조회(전부다잖아?)
    // 리프레시 토큰이 아니다? - DB제거 필요 없음 - return으로 끝냄
    // 마무리로는 프론트에서 토큰 제거하면 모든 토큰 없음
    @Transactional
    public void logout(String refreshTokenValue) {
        try {
            // JWT 자체의 유효성 검증
            var claims = tokenProvider.parse(refreshTokenValue);
            // Refresh Token 인지 검증
            if (!tokenProvider.isRefreshToken(claims)) {return;}
            // DB에서 토큰 조회 - 토큰 유효하면 - 취소시킴
            refreshTokenRepository.findByToken(refreshTokenValue).filter(RefreshTokens::isUsable).ifPresent(RefreshTokens::revoke);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.TOKEN_EXPIRED && e.getErrorCode() != ErrorCode.TOKEN_INVALID) {throw e;}
        }
    }

    // 비밀번호 재설정 안내(버튼)
    // 비밀번호 찾기 - 이메일 발송 - 숫자 6자리 복호화해서 전달
    // DB에서 비밀번호 변경하고 - 로그인 하고 - 비밀번호 변경 권장
    @Transactional
    public void resetPassword(String email){
        String tempPassword = tempPasswordCreate();
        Members member = findByEmailMember(email);
        emailService.sendEmail(email,"임시 비밀번호 발급","임시 비밀버호는 : "+ tempPassword + "입니다.");
        member.setPasswordHash(passwordEncoder.encode(tempPassword));
    }

    // 임시 비밀번호 생성
    private String tempPasswordCreate() {return PasswordGenerator.createTempPassword();}


    //새 리프레시 토큰을 발급하고 기존 것을 폐기
    @Transactional
    public TokenResponse refresh(String refreshTokenValue, HttpServletRequest httpRequest) {
        var claims = tokenProvider.parse(refreshTokenValue);
        if (!tokenProvider.isRefreshToken(claims)) {throw new BusinessException(ErrorCode.TOKEN_INVALID);}

        RefreshTokens stored = refreshTokenRepository.findByToken(refreshTokenValue).orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        if (!stored.isUsable()) {
        // 이미 폐기된 토큰의 재사용 — 탈취 의심. 전부 끊는다.
            log.warn("폐기된 refresh 토큰 재사용 시도: memberId={}", stored.getMember().getId());
            refreshTokenRepository.revokeAllByMember(stored.getMember().getId());
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Members member = stored.getMember();
        if (member.getMemberStatus() == MemberStatus.DELETED) {throw new BusinessException(ErrorCode.MEMBER_DELETED);}

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
        return new TokenResponse(accessToken);
    }//refresh

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String header(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

}
