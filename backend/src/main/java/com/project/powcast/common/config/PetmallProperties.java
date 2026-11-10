package com.project.powcast.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "petmall")
public record PetmallProperties(
        Jwt jwt,
        Cors cors,
        Ai ai,
        Order order,
        Upload upload,
        Ui ui
) {
    public record Jwt(
            String secret,
            long accessTokenValiditySeconds,
            long refreshTokenValiditySeconds,
            String issuer
    ) {}
    public record Cors(
            List<String> allowedOrigins,
            String allowedOriginPattern,
            boolean allowCredentials,
            long maxAge,
            List<String> exposedHeaders
    ) {}
    public record Ai(
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs,
            boolean fallbackEnabled,
            CircuitBreaker circuitBreaker
    ) { public record CircuitBreaker(int failureThreshold, int openDurationSeconds) {}}

    public record Order(
            long freeShippingThreshold,
            long defaultShippingFee,
            long islandExtraFee,
            int pendingExpireHours,
            int reviewWritableDays
    ) {}

    public record Ui(boolean tailwindCdn) {}
    public record Upload(
            String baseDir,
            String publicPath,
            List<String> allowedExtensions){}
}
