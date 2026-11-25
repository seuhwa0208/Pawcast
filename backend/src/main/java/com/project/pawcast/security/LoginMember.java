package com.project.pawcast.security;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class LoginMember implements UserDetails {
    private final Long id;
    private final String email;
    private final String name;
    private final String role;
    private final boolean active;

    public LoginMember(Long id, String email, String name, String role, boolean active) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.active = active;
    }

    /** 토큰 인증용: 비밀번호 없이 만든다. */
    public static LoginMember ofToken(Long id, String email, String role) {
        // DB와 발급 토큰은 MANAGER를 쓰지만, 접두사가 포함된 기존 토큰도
        // ROLE_ROLE_MANAGER 권한으로 잘못 변환되지 않도록 정규화합니다.
        String normalizedRole = role != null && role.startsWith("ROLE_")
                ? role.substring("ROLE_".length())
                : role;
        return new LoginMember(id, email, "", normalizedRole, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }
}

