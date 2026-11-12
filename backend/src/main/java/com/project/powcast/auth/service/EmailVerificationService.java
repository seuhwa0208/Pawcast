package com.project.powcast.auth.service;

import com.project.powcast.auth.entity.EmailVerification;
import com.project.powcast.auth.repository.EmailVerificationRepository;
import com.project.powcast.common.email.EmailService;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {
    private final EmailVerificationRepository emailVerificationRepo;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    // 이메일 인증 발송
    // 버튼을 누르면 이메일서비스가 이메일을 보내기전 코드를 저장하고, 이메일을 보냄
    // 이후 저장한 코드와 보낸 코드가 일치하면 인증 시간을 기록함
    // 만료시간은 보낸 시간 값 + 5분? 10분?

    @Transactional
    public void sendVerifyEmail(String email){
        
        //이메일 보내기전 이미 이 이메일로 가입된 회원이 있는지 검사
        if(memberRepository.existsByEmail(email)){throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);}
        emailVerificationRepo.deleteByEmail(email);

        //이메일 코드 발생 및 변환
        String verifyCode = String.format("%06d", secureRandom.nextInt(1000000));
        String encodeCode = passwordEncoder.encode(verifyCode);
        
        //이메일 보내기전 테이블에 저장
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setEmail(email);
        emailVerification.setVerificationCode(encodeCode);
        LocalDateTime dateNow = LocalDateTime.now();
        emailVerification.setCreatedAt(dateNow);
        emailVerification.setExpiresAt(dateNow.plusMinutes(5));
        emailVerificationRepo.save(emailVerification);

        //이메일 보내기
        emailService.sendEmail(email,"회원가입 이메일 인증 번호","인증번호는 " + verifyCode + "입니다.");
    }
}
