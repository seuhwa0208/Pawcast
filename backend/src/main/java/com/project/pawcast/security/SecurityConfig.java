package com.project.pawcast.security;

import lombok.RequiredArgsConstructor;
import com.project.pawcast.common.config.PetmallProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
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
                        .requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll()   // 구글 소셜 로그인 관련 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/logs/**").permitAll()
                        // 회원 목록·상태·역할 변경은 관리자 전용이다. 반드시 포괄 /api/admin/** 규칙보다 먼저 둔다.
                        .requestMatchers("/api/admin/members/**", "/api/admin").hasRole("ADMIN")
                        // 모델 재학습은 운영 데이터에 영향을 주므로 관리자만 실행할 수 있다.
                        .requestMatchers("/api/admin/ai/**").hasRole("ADMIN")
                        // 매출·주문·상품·공지·Q&A 운영은 관리자와 매니저가 함께 사용한다.
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

        config.setAllowedOrigins(petmallProperties.cors().allowedOrigins());

        config.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
