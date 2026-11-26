package com.project.pawcast.security;

import lombok.RequiredArgsConstructor;
import com.project.pawcast.common.config.PetmallProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final PetmallProperties petmallProperties;

    // 연동 오류로 추가
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
     * JWT REST API
     * 1) /api/**  : 무상태(STATELESS) + JWT + CSRF 끔 + JSON 401
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 토큰 인증이므로 CSRF 토큰이 필요 없다(쿠키를 자동 전송하지 않는다).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 만료·누락 토큰은 401, 인증되었지만 권한이 없으면 403으로 구분합니다.
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // QnA 관리 목록은 공개 QnA 조회 규칙보다 먼저 역할을 확인합니다.
                        .requestMatchers(HttpMethod.GET, "/api/qna/manage", "/api/qna/manage/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        // 공개 조회 API는 회원·매니저 모두 계속 사용할 수 있습니다.
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**", "/api/categories/**",
                                "/api/brands/**", "/api/notices/**",
                                "/api/reviews/**", "/api/qna/**",
                                "/api/banners/**", "/api/recommendations/popular",
                                "/api/search/**","/uploads/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/logs/**").permitAll()
                        // 비회원 주문 생성과 결제 승인은 기존 공개 계약을 유지합니다.
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/*/payments/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/guest").permitAll()
                        // 회원 역할 변경과 AI 모델 재학습 진입점은 ADMIN 전용입니다.
                        .requestMatchers("/api/admin/members/**", "/api/admin").hasRole("ADMIN")
                        .requestMatchers("/api/admin/ai/**").hasRole("ADMIN")
                        // 매출·대시보드는 민감한 경영 정보이므로 ADMIN만 조회한다.
                        .requestMatchers("/api/admin/sales/**", "/api/admin/dashboard/**").hasRole("ADMIN")
                        // 매니저는 요청된 운영 영역만 허용합니다. 새 관리자 경로는 아래 기본 ADMIN 규칙에 걸립니다.
                        .requestMatchers(
                                "/api/admin/refunds", "/api/admin/refunds/**",
                                "/api/admin/products/**",
                                "/api/admin/variants/**",
                                "/api/orders/admin", "/api/orders/admin/**",
                                "/api/notices", "/api/notices/**",
                                "/api/inquiries/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 허용 목록에 없는 인증 API는 매니저에게 403으로 닫고,
                        // USER와 ADMIN의 기존 로그인 기반 API 동작은 유지합니다.
                        .anyRequest().access((authentication, context) -> {
                            var current = authentication.get();
                            if (current == null
                                    || current instanceof AnonymousAuthenticationToken
                                    || !current.isAuthenticated()) {
                                return new AuthorizationDecision(false);
                            }

                            boolean isManager = current.getAuthorities().stream()
                                    .anyMatch(authority ->
                                            "ROLE_MANAGER".equals(authority.getAuthority()));
                            return new AuthorizationDecision(!isManager);
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(petmallProperties.cors().allowedOrigins());

        config.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}



