package com.project.pawcast.review.controller;

import com.project.pawcast.review.dto.*;
import com.project.pawcast.review.service.ReviewService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    // 상품별 공개 리뷰 목록 조회
    @GetMapping("/products/{productId}/reviews")
    public ProductReviewsResponse getReviews(@PathVariable Long productId) {
        return reviewService.getReviews(productId);
    }

    // 공개 리뷰 단건 조회
    @GetMapping("/reviews/{reviewId}")
    public ReviewResponse getReview(@PathVariable Long reviewId) {
        return reviewService.getReview(reviewId);
    }

    // 리뷰 등록
    @PostMapping("/reviews")
    public ReviewResponse createReview(@AuthenticationPrincipal LoginMember member, @Valid @RequestBody ReviewCreateRequest request) {
        return reviewService.createReview(member.getId(), request);
    }

    // 리뷰 수정
    @PatchMapping("/reviews/{reviewId}")
    public ReviewResponse updateReview(@AuthenticationPrincipal LoginMember member, @PathVariable Long reviewId, @Valid @RequestBody ReviewUpdateRequest request) {
        return reviewService.updateReview(reviewId, member.getId(), request);
    }

    // 리뷰 삭제
    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(@AuthenticationPrincipal LoginMember member, @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId, member.getId());
    }
}