package com.project.pawcast.review.service;

import com.project.pawcast.review.client.SentimentClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSentimentService {

    private static final Set<String> ALLOWED_SENTIMENTS =
            Set.of("POSITIVE", "NEGATIVE", "NEUTRAL");

    private final SentimentClient sentimentClient;
    private final ReviewSentimentWriter sentimentWriter;

    public void analyzeAndSave(Long reviewId, String content) {
        // 본문이 없으면 분석x
        if (content == null || content.isBlank()) {
            return;
        }

        try {
            // 파이썬 API 호출
            var result = sentimentClient.analyze(reviewId, content);

            // 분석 결과 검증
            validateResult(result);

            // DB 저장
            sentimentWriter.save(result, content);

        } catch (RuntimeException e) {
            // 분석 실패가 리뷰 작성 성공에 영향을 주지 않도록 처리
            // 실패 결과를 NEUTRAL로 저장x
            log.error(
                    "리뷰 감정분석 처리 실패. reviewId={}",
                    reviewId,
                    e
            );
        }
    }

    private void validateResult(
            SentimentClient.SentimentResponse result
    ) {
        String sentiment = result.sentiment();

        if (sentiment == null
                || !ALLOWED_SENTIMENTS.contains(sentiment)) {
            throw new IllegalStateException(
                    "감정 분류값이 올바르지 않습니다."
            );
        }

        BigDecimal confidence = result.confidence();

        if (confidence == null
                || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException(
                    "confidence는 0 이상 1 이하여야 합니다."
            );
        }

        if (result.modelName() == null
                || result.modelName().isBlank()) {
            throw new IllegalStateException(
                    "분석 모델명이 없습니다."
            );
        }
    }
}