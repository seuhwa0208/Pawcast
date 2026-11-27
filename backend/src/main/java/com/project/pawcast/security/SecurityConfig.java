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

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    /*
     * JWT REST API
     * /api/** : STATELESS + JWT + CSRF 비활성화
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .securityMatcher("/api/**")

                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // CORS Preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // QnA 관리자 조회
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/qna/manage",
                                "/api/qna/manage/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // 공개 조회 API
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories/**",
                                "/api/brands/**",
                                "/api/notices/**",
                                "/api/reviews/**",
                                "/api/qna/**",
                                "/api/banners/**",
                                "/api/recommendations/popular",
                                "/api/search/**",
                                "/uploads/**",
                                "/images/**"
                        ).permitAll()

                        // 인증 및 로그
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/logs/**"
                        ).permitAll()

                        // 비회원 주문 및 결제
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders",
                                "/api/orders/*/payments/confirm"
                        ).permitAll()

                        // 비회원 주문 조회
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders/guest"
                        ).permitAll()

                        // 관리자 전용 회원 관리
                        .requestMatchers(
                                "/api/admin/members/**",
                                "/api/admin"
                        ).hasRole("ADMIN")

                        // AI 모델 관리
                        .requestMatchers(
                                "/api/admin/ai/**"
                        ).hasRole("ADMIN")

                        // 매출 및 대시보드
                        .requestMatchers(
                                "/api/admin/sales/**",
                                "/api/admin/dashboard/**"
                        ).hasRole("ADMIN")

                        // 관리자 및 매니저 운영 권한
                        .requestMatchers(
                                "/api/admin/refunds",
                                "/api/admin/refunds/**",
                                "/api/admin/products/**",
                                "/api/admin/variants/**",
                                "/api/orders/admin",
                                "/api/orders/admin/**",
                                "/api/notices",
                                "/api/notices/**",
                                "/api/inquiries/**"
                        ).hasAnyRole("ADMIN", "MANAGER")

                        // 기타 관리자 API
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // 나머지 인증 API
                        .anyRequest().access((authentication, context) -> {

                            var current = authentication.get();

                            if (
                                    current == null
                                            || current instanceof AnonymousAuthenticationToken
                                            || !current.isAuthenticated()
                            ) {
                                return new AuthorizationDecision(false);
                            }

                            boolean isManager = current
                                    .getAuthorities()
                                    .stream()
                                    .anyMatch(authority ->
                                            "ROLE_MANAGER".equals(
                                                    authority.getAuthority()
                                            )
                                    );

                            return new AuthorizationDecision(!isManager);
                        })
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .httpBasic(basic -> basic.disable())

                .formLogin(form -> form.disable());

        return http.build();
    }

    /*
     * CORS 설정
     *
     * 로컬 개발 환경 허용
     * 외부 배포는 Nginx 동일 출처 프록시 사용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.trycloudflare.com"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}