package com.project.pawcast.review.service;

import com.project.pawcast.review.client.SentimentClient;
import com.project.pawcast.review.entity.ProductReview;
import com.project.pawcast.review.entity.ReviewSentiment;
import com.project.pawcast.review.repository.ProductReviewRepository;
import com.project.pawcast.review.repository.ReviewSentimentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewSentimentWriter {

    private final ProductReviewRepository reviewRepository;
    private final ReviewSentimentRepository sentimentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            SentimentClient.SentimentResponse result,
            String analyzedContent
    ) {
        // 결과 저장 중에는 같은 리뷰의 수정·삭제 막기
        ProductReview review = reviewRepository
                .findByIdForUpdate(result.reviewId())
                .orElse(null);

        // 분석하는 동안 리뷰가 삭제됐다면 저장x
        if (review == null) {
            return;
        }

        // 분석한 본문과 현재 본문이 다르면 이전 결과를 저장x
        if (!Objects.equals(review.getContent(), analyzedContent)) {
            return;
        }

        // 기존 결과가 있으면 갱신, 없으면 새로 생성
        ReviewSentiment sentiment = sentimentRepository
                .findByReviewId(review.getId())
                .orElseGet(ReviewSentiment::new);

        sentiment.setReview(review);
        sentiment.setSentiment(result.sentiment());
        sentiment.setConfidence(result.confidence());
        sentiment.setModelName(result.modelName());
        sentiment.setAnalyzedAt(LocalDateTime.now());

        sentimentRepository.save(sentiment);
    }
}