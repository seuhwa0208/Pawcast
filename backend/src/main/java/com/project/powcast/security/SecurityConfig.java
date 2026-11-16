package com.project.powcast.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    /*
     * JWT REST API
     * 1) /api/**  : 무상태(STATELESS) + JWT + CSRF 끔 + JSON 401
     */
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 토큰 인증이므로 CSRF 토큰이 필요 없다(쿠키를 자동 전송하지 않는다).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // exceptionHandler 사용 안하는 방향으로
                //.exceptionHandling(handler -> handler.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트는 항상 통과
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 공개 조회
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**", "/api/categories/**",
                                "/api/brands/**", "/api/notices/**",
                                "/api/reviews/**", "/api/qna/**",
                                "/api/banners/**", "/api/recommendations/popular",
                                "/api/search/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/logs/**").permitAll()
                        // 관리자
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        // 그 밖의 API 는 로그인 필요
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
