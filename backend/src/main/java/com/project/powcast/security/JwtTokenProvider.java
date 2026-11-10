package com.project.powcast.security;


import com.project.powcast.common.config.PetmallProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/**
 * JWT 발급·검증.
 *
 * <p>토큰에는 회원 식별자(sub), 권한(role), 이메일만 담는다. 이름이나
 * 등급처럼 바뀌는 값을 넣으면 토큰이 만료될 때까지 낡은 값이 돌아다닌다.
 *
 * <p>Access 30분 / Refresh 14일. Refresh 토큰은 DB(refresh_tokens)에
 펫몰 구축 매뉴얼 · 94 / 320
 * 저장해 **서버에서 취소할 수 있게** 한다. JWT 는 그 자체로 취소가 안 되므로,
 * 로그아웃·비밀번호 변경·탈퇴 시 저장된 refresh 를 무효화하는 방식으로 막는다.
 */
@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private final SecretKey key;
    private final PetmallProperties.Jwt config;

    public JwtTokenProvider(PetmallProperties properties) {
        this.config = properties.jwt();
        byte[] secret = config.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
        // HS256 은 256비트 이상을 요구한다. 짧은 키로 기동되면 운영 중
        // 첫 로그인에서야 터지므로, 기동 시점에 막는다.
            throw new IllegalStateException(
                    "petmall.jwt.secret 은 32바이트(256비트) 이상이어야 합니다. 현재 "
                            + secret.length + "바이트");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String createAccessToken(Long memberId, String email, String role) {
        return create(memberId, email, role, TYPE_ACCESS, config.accessTokenValiditySeconds());
    }

    public String createRefreshToken(Long memberId, String email, String role) {
        return create(memberId, email, role, TYPE_REFRESH, config.refreshTokenValiditySeconds());
    }

    private String create(Long memberId, String email, String role, String type, long validitySeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validitySeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuer(config.issuer())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public long accessTokenValiditySeconds() {return config.accessTokenValiditySeconds();}
}
