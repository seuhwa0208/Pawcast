package com.project.pawcast.review.dto;

import com.project.pawcast.review.entity.ProductReview;

import java.time.LocalDateTime;

public record BestReviewResponse(

        Long id,
        int rating,
        String content,
        String writerId,
        LocalDateTime createdAt

) {

    public static BestReviewResponse from(ProductReview review) {
        return new BestReviewResponse(
                review.getId(),
                review.getRating(),
                review.getContent(),
                review.getMember().getUserId(),
                review.getCreatedAt()
        );
    }
}
