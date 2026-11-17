package com.project.powcast.review.event;

public record ReviewAnalysisRequested(
        Long reviewId,
        String content
) {}