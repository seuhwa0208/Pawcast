package com.project.powcast.review.controller;

import com.project.powcast.review.dto.ProductReviewsResponse;
import com.project.powcast.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ProductReviewsResponse getReviews(@PathVariable Long productId) {
        return reviewService.getReviews(productId);
    }
}
