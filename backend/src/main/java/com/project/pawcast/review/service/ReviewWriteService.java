package com.project.pawcast.review.service;

import com.project.pawcast.review.dto.ReviewCreateRequest;
import com.project.pawcast.review.dto.ReviewResponse;
import com.project.pawcast.review.dto.ReviewUpdateRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewWriteService {

    private final ReviewModerationService moderationService;
    private final ReviewService reviewService;

    // 리뷰 등록
    public ReviewResponse createReview(
            Long memberId,
            ReviewCreateRequest request
    ) {
        // 1. 파이썬에서 본문 검사
        moderationService.validateContent(request.content());

        // 2. 통과하면 기존 등록 기능 실행
        return reviewService.createReview(memberId, request);
    }

    // 리뷰 수정
    public ReviewResponse updateReview(
            Long reviewId,
            Long memberId,
            ReviewUpdateRequest request
    ) {
        // 1. 수정 요청의 본문 검사
        moderationService.validateContent(request.content());

        // 2. 통과하면 기존 수정 기능 실행
        return reviewService.updateReview(
                reviewId,
                memberId,
                request
        );
    }
}