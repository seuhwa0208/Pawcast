package com.project.powcast.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        // Authorization 헤더에서 JWT 추출
        String token = resolveToken(request);
        // 토큰이 존재하고 아직 인증되지 않은 경우
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = tokenProvider.parseQuietly(token);
            // refresh 토큰으로는 API 를 호출할 수 없다. 둘을 구분하지 않으면
            // 긴 수명의 refresh 가 사실상 무기한 access 로 쓰인다.
            // 유효한 Access Token인지 확인
            if (claims != null && !tokenProvider.isRefreshToken(claims)) {
                // 인증 주체 생성
                LoginMember principal = LoginMember.ofToken(
                        tokenProvider.memberId(claims),
                        tokenProvider.email(claims),
                        tokenProvider.role(claims));
                
                // 인증 객체 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                // SecurityContext에 인증 정보 등록
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                }
            }
            // 다음 필터 실행
            chain.doFilter(request, response);
        }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER);
        if (bearerToken != null && bearerToken.startsWith(PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

}