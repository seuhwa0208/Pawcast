package com.project.pawcast.review.event;

public record ReviewAnalysisRequested(
        Long reviewId,
        String content
) {}