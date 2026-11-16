package com.project.powcast.security;

import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Members member = memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다."));

        if (member.getStatus() == Members.Status.INACTIVE) {throw new UsernameNotFoundException("비활성화된 계정입니다.");}
        if (member.getStatus() == Members.Status.DELETED) {throw new UsernameNotFoundException("계정을 찾을 수 없습니다.");}

        return new LoginMember(
                member.getId(),
                member.getEmail(),
                member.getPasswordHash(),
                member.getName(),
                member.getRole().name(),
                member.getStatus() == Members.Status.ACTIVE);
    }
}

