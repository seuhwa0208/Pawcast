package com.project.pawcast.qna.dto;

import com.project.pawcast.qna.entity.Qna;
import com.project.pawcast.qna.entity.QnaType;

import java.time.LocalDateTime;

public record QnaResponse(
        Long qnaId,
        Long memberId,
        String memberName,
        Long productId,
        String productName,
        QnaType type,
        String title,
        String content,
        boolean isPrivate,
        LocalDateTime createdAt
) {
    public static QnaResponse from(Qna qna) {
        return new QnaResponse(
                qna.getId(),
                qna.getMember() == null ? null : qna.getMember().getId(),
                qna.getMember() == null ? null : qna.getMember().getName(),
                qna.getProduct() == null ? null : qna.getProduct().getId(),
                qna.getProduct() == null ? null : qna.getProduct().getName(),
                qna.getType(),
                qna.getTitle(),
                qna.getContent(),
                qna.isPrivate(),
                qna.getCreatedAt()
        );
    }
}