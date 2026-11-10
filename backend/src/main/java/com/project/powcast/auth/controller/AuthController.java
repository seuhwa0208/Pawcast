package com.project.powcast.auth.controller;

import com.project.powcast.auth.dto.SignUpRequest;
import com.project.powcast.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;

import org.springframework.security.oauth2.jose.jws.JwsHeader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MemberService memberServiceService;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignUpRequest request) {

        authService.signup(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.username(),
                                request.password()
                        )
                );

        Instant now = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer("security-vue-lab")
                        .subject(
                                authentication.getName()
                        )
                        .issuedAt(now)
                        .expiresAt(
                                now.plusSeconds(3600)
                        )
                        .claim(
                                "roles",
                                authentication
                                        .getAuthorities()
                                        .stream()
                                        .map(a ->
                                                a.getAuthority()
                                        )
                                        .toList()
                        )
                        .build();

        String token =
                jwtEncoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(
                                        SignatureAlgorithm.RS256
                                ).build(),
                                claims
                        )
                ).getTokenValue();

        return new TokenResponse(token);
    }
}