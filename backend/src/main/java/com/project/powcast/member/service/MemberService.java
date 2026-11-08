package com.project.powcast.member.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.EmailSendException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.dto.MemberRequest;
import com.project.powcast.member.dto.MemberResponse;
import com.project.powcast.member.email.EmailVerification;
import com.project.powcast.member.email.EmailVerificationRepository;
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
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    //아이디 중복 검사
    public boolean checkUserId(String userId){
        return memberRepository.existsByUserId(userId);
    }

    //이메일 인증 검증
    //이메일 기반으로 코드가 일치하는지 비교
    //만족하면 인증된 시간 저장
    //인증만 하고 가입을 안하게되면? 다시 가입할때 이미 인증이 완료된 상태잖아? 그러면 안되니깐
    //이메일이 인증이 완료됬는데 이 이메일 기반으로 회원아이디가 없으면? 인증완료된 정보를 파기해야함 - 언제? - 다시 이메일 검증할때
    // 이메일 보내기전에 이 이메일로 가입된 회원정보가 있나 확인하고 있으면 막고, 없으면 인증정보 파기후 다시 생성 - 이메일인증서비스에서 파기하고 새로만듬
    // 여기선 코드 검사하고 성공하면 완료시간만 업데이트하면 될듯?
    @Transactional
    public boolean checkVerifyEmail(String email, String checkCode){
        EmailVerification emailVerification = emailVerificationRepository.findByEmail(email).orElseThrow(() -> new EmailSendException(ErrorCode.NOT_FOUND_EMAIL_VERIFICATION));
        
        //인증 완료된 이메일인지 확인
        
        // 이메일 코드 날짜 만료 확인
        if (!LocalDateTime.now().isBefore(emailVerification.getExpiresAt())) { throw new BusinessException(ErrorCode.EXPIRED_EMAIL_CODE);}
        
        // 이메일 코드 체크 - 일치하지 않으면 - false 반환
        if(!passwordEncoder.matches(checkCode,emailVerification.getVerificationCode())){return false;}
        
        // 성공하면 인증 날짜 저장하고 true 반환
        emailVerification.setVerifiedAt(LocalDateTime.now());
        return true;
    }


    //회원가입
//    @Transactional
//    public MemberResponse register(MemberRequest request){
//        // 아이디 중복 검사
//        if (checkUserId(request.getUserId())){
//            throw new BusinessException(ErrorCode.DUPLICATE_USERID);
//        }
//        // 이메일 검증이 유효한지
//    }
}
