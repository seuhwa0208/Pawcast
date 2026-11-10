package com.project.powcast.auth.service;


import com.project.powcast.auth.dto.LoginRequest;
import com.project.powcast.auth.dto.LoginResponse;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.EmailSendException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.auth.dto.SignUpRequest;
import com.project.powcast.member.email.EmailVerification;
import com.project.powcast.member.email.EmailVerificationRepository;
import com.project.powcast.member.entity.MemberRole;
import com.project.powcast.member.entity.MemberStatus;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    //아이디 중복 검사
    public boolean checkUserId(String userId){
        return memberRepository.existsByUserId(userId);
    }

    // findByEmail - 이메일로 이메일 검증 정보 확인
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
        if(checkVerificationEmail(email).getVerifiedAt() != null) {throw new BusinessException(ErrorCode.VERIFICATION_COMPLETED_EMAIL);}

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
        if (checkUserId(request.getUserId())){throw new BusinessException(ErrorCode.DUPLICATE_USERID);}

        // 이메일 검증이 유효한지
        // 적혀있는 이메일 인증 완료됫는지 검사 및 방금 전 이메일과 일치하는가?
        EmailVerification emailVerification = checkVerificationEmail(request.getEmail());  // 이메일 검증 정보 확인(유무 확인)
        // 이메일이 인증 되었는지
        if(emailVerification.getVerifiedAt() == null){throw new BusinessException(ErrorCode.NOT_VERIFIED_EMAIL);}
        // 이미 가입된 이메일인지(중복검사)
        if (memberRepository.existsByEmail(request.getEmail())){throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);}

        // 비밀번호 일치 확인
        if(!request.getPasswordHash().equals(request.getCheckPasswordHash())) {throw new BusinessException(ErrorCode.CHECK_PASSWORD_ERROR);}

        // 이용 약관 검사
        if(!request.isAgreedTerms()) {throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);}

        // 개인 정보 약관 검사
        if(!request.isAgreedPrivacy()) {throw new BusinessException(ErrorCode.REQUIRED_PRIVACY_AGREEMENT);}

        // 전자 금융 거래 약관 검사
        if(!request.isAgreedEfinance()) {throw new BusinessException(ErrorCode.REQUIRED_EFINANCE_AGREEMENT);}

        // 아이디 저장
        member.setUserId(request.getUserId());
        //비밀번호 암호화 및 저장
        member.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));
        //이름 저장
        member.setUsername(request.getUsername());
        //이메일 저장
        member.setEmail(request.getEmail());
        //전화번호 저장
        member.setTel(request.getTel());
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

    // 로그인 인증

    public LoginResponse login(LoginRequest request){
        // 계정조회
        
        // 비밀번호 확인
        
        //계정 활성화 상태 확인
        
        //역할에 맞는 인증정보 발급
    }
}
