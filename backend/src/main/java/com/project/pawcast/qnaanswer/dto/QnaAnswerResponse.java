package com.project.pawcast.qnaanswer.dto;

import com.project.pawcast.qnaanswer.entity.QnaAnswer;

import java.time.LocalDateTime;

public record QnaAnswerResponse(
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaAnswerResponse from(QnaAnswer qnaAnswer) {
        return new QnaAnswerResponse(
                qnaAnswer.getContent(),
                qnaAnswer.getCreatedAt(),
                qnaAnswer.getUpdatedAt()
        );
    }
}
