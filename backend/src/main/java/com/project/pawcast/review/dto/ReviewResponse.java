package com.project.pawcast.review.dto;

import com.project.pawcast.review.entity.ProductReview;
import com.project.pawcast.review.entity.ReviewImage;
import com.project.pawcast.review.entity.ReviewSentiment;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(

        Long id,
        Long productId,
        String writerName,
        Integer rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ReviewImageResponse> images,
        ReviewSentimentResponse sentiment
) {

    public static ReviewResponse from(
            ProductReview review,
            List<ReviewImage> images
    ) {
        return from(review, images, null);
    }
    public static ReviewResponse from(
            ProductReview review,
            List<ReviewImage> images,
            ReviewSentiment sentiment
    ) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getMember().getName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                images.stream()
                        .map(ReviewImageResponse::from)
                        .toList(),
                sentiment == null
                        ? null
                        : ReviewSentimentResponse.from(sentiment)
        );
    }
}