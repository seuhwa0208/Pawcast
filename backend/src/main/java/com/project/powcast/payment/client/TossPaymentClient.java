package com.project.powcast.payment.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private final RestClient restClient = RestClient.builder().baseUrl("https://api.tosspayments.com/v1").build();

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    private String getAuthorization() {
        String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedKey;
    }

    // 결제 승인
    public Map<String, Object> approve(String paymentKey, String orderId, Long amount) {

        String encodedKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        return restClient.post().uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, getAuthorization())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    // 결제 취소
    public Map<String, Object> cancel(String paymentKey, String cancelReason) {
        return restClient.post()
                .uri("/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, getAuthorization())
                .body(Map.of("cancelReason", cancelReason))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

}