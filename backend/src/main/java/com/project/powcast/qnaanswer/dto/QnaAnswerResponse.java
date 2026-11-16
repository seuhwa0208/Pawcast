package com.project.powcast.qnaanswer.dto;

import com.project.powcast.notice.dto.NoticeResponse;
import com.project.powcast.notice.entity.Notice;
import com.project.powcast.qnaanswer.entity.QnaAnswer;

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
