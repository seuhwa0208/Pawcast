package com.project.powcast.review.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SentimentClient {

    private final RestClient restClient;

    public SentimentClient(
            RestClient.Builder builder,
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl
    ) {
        // 파이썬 서버 연결 대기 시간
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        // 분석 응답 대기 시간
        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public SentimentResponse analyze(Long reviewId, String content) {
        SentimentRequest request =
                new SentimentRequest(reviewId, content);

        SentimentResponse response = restClient.post()
                .uri("/internal/sentiments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SentimentResponse.class);

        if (response == null) {
            throw new IllegalStateException("감정분석 응답이 없습니다.");
        }

        if (!reviewId.equals(response.reviewId())) {
            throw new IllegalStateException("분석 결과의 리뷰 ID가 다릅니다.");
        }

        return response;
    }

    // 자바 -> 파이썬으로 보내는 데이터
    public record SentimentRequest(
            Long reviewId,
            String content
    ) {}

    // 파이썬 -> 자바로 받는 데이터
    public record SentimentResponse(
            Long reviewId,
            String sentiment,
            BigDecimal confidence,
            String modelName
    ) {}
}