package com.project.powcast.review.dto;

import com.project.powcast.review.entity.ProductReview;
import com.project.powcast.review.entity.ReviewImage;

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
        List<ReviewImageResponse> images

) {

    public static ReviewResponse from(
            ProductReview review,
            List<ReviewImage> images
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
                        .toList()
        );
    }
}