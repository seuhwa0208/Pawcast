package com.project.pawcast.review.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductReviewsResponse(

        long reviewCount,
        BigDecimal averageRating,
        List<ReviewResponse> reviews

) {
}