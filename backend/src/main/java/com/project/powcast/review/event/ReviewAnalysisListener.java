package com.project.powcast.review.event;

import com.project.powcast.review.service.ReviewSentimentService;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewAnalysisListener {

    private final ReviewSentimentService sentimentService;

    @Async("reviewAnalysisExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(ReviewAnalysisRequested event) {
        sentimentService.analyzeAndSave(
                event.reviewId(),
                event.content()
        );
    }
}