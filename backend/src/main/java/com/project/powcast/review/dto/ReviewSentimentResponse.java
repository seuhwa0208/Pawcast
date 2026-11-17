package com.project.powcast.review.dto;

import com.project.powcast.review.entity.ReviewSentiment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewSentimentResponse(
        String sentiment,
        BigDecimal confidence,
        String modelName,
        LocalDateTime analyzedAt
) {

    public static ReviewSentimentResponse from(
            ReviewSentiment entity
    ) {
        return new ReviewSentimentResponse(
                entity.getSentiment(),
                entity.getConfidence(),
                entity.getModelName(),
                entity.getAnalyzedAt()
        );
    }
}