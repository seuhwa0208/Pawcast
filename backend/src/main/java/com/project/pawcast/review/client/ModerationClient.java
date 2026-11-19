package com.project.pawcast.review.client;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ModerationClient {

    private final RestClient restClient;

    public ModerationClient(
            RestClient.Builder builder,
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public ModerationResponse check(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        try {
            ModerationResponse response = restClient.post()
                    .uri("/internal/moderation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ModerationRequest(content))
                    .retrieve()
                    .body(ModerationResponse.class);

            // 응답이 없거나 allowed 필드가 없으면 검사 실패
            if (response == null || response.allowed() == null) {
                throw new BusinessException(
                        ErrorCode.MODERATION_UNAVAILABLE
                );
            }

            return response;

        } catch (RestClientException e) {
            // 연결 실패, 시간 초과, HTTP 오류, 응답 변환 실패
            throw new BusinessException(
                    ErrorCode.MODERATION_UNAVAILABLE
            );
        }
    }

    // 자바 → 파이썬
    public record ModerationRequest(
            String content
    ) {}

    // 파이썬 → 자바
    public record ModerationResponse(
            Boolean allowed
    ) {}
}