package com.project.pawcast.review.service;

import com.project.pawcast.review.dto.ReviewCreateRequest;
import com.project.pawcast.review.dto.ReviewResponse;
import com.project.pawcast.review.dto.ReviewUpdateRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewWriteService {

    private final ReviewService reviewService;

    // 리뷰 등록
    public ReviewResponse createReview(
            Long memberId,
            ReviewCreateRequest request
    ) {
        // ReviewService 내부 Java ProfanityFilter 검증 후 등록
        return reviewService.createReview(memberId, request);
    }

    // 리뷰 수정
    public ReviewResponse updateReview(
            Long reviewId,
            Long memberId,
            ReviewUpdateRequest request
    ) {
        // ReviewService 내부 Java ProfanityFilter 검증 후 수정
        return reviewService.updateReview(
                reviewId,
                memberId,
                request
        );
    }
}
