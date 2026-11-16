package com.project.powcast.review.dto;

import com.project.powcast.review.entity.ProductReview;

import java.time.LocalDateTime;

public record BestReviewResponse(

        Long id,
        int rating,
        String content,
        String writerName,
        LocalDateTime createdAt

) {

    public static BestReviewResponse from(ProductReview review) {
        return new BestReviewResponse(
                review.getId(),
                review.getRating(),
                review.getContent(),
                review.getMember().getName(),
                review.getCreatedAt()
        );
    }
}