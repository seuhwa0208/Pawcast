package com.project.powcast.review.dto;
import java.math.BigDecimal;
import java.util.List;
public record ProductReviewsResponse(long reviewCount, BigDecimal averageRating, List<BestReviewResponse> reviews) {}
