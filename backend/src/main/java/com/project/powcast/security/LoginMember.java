package com.project.powcast.security;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class LoginMember implements UserDetails {
    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final String role;
    private final boolean active;

    public LoginMember(Long id, String email, String password, String name, String role, boolean active) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.active = active;
    }

    /** 토큰 인증용: 비밀번호 없이 만든다. */
    public static LoginMember ofToken(Long id, String email, String role) {
        return new LoginMember(id, email, "", "", role, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }
}
