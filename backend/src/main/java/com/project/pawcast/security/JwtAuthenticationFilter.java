package com.project.pawcast.security;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private final JwtTokenProvider tokenProvider;
    private final MembersRepository membersRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, MembersRepository membersRepository) {
        this.tokenProvider = tokenProvider;
        this.membersRepository = membersRepository;
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
                Long memberId = tokenProvider.memberId(claims);
                if (membersRepository.existsByIdAndStatusNot(memberId, Members.Status.DELETED)) {
                    LoginMember principal = LoginMember.ofToken(
                            memberId, tokenProvider.email(claims), tokenProvider.role(claims));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
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
