package com.project.pawcast.common.init;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InitialDataInitializer implements CommandLineRunner {

    private final MembersRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 관리자 계정
        createMember("admin", "관리자", "admin@example.com", "010-0000-0001", "1111", Members.Role.ADMIN);
        // 매니저 계정
        createMember("manager", "매니저", "manager@example.com", "010-0000-0002", "1111", Members.Role.MANAGER);
        // 일반 회원 계정
        createMember("user", "일반회원", "user@example.com", "010-0000-0003", "1111", Members.Role.USER);
    }

    private void createMember(String userId, String name, String email, String tel, String password, Members.Role role)
    {
        if (memberRepository.existsByUserId(userId)) {return;}

        memberRepository.save(Members.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .tel(tel)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .status(Members.Status.ACTIVE)
                .isAgreedTerms(true)
                .isAgreedPrivacy(true)
                .isAgreedEfinance(true)
                .isAgreedMarketing(false)
                .build());
    }
}

