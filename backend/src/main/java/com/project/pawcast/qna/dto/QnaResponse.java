package com.project.pawcast.qna.dto;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.notice.dto.NoticeResponse;
import com.project.pawcast.notice.entity.Notice;
import com.project.pawcast.product.entity.Product;
import com.project.pawcast.qna.entity.Qna;
import com.project.pawcast.qna.entity.QnaType;

public record QnaResponse(
        Members member,
        Product product,
        QnaType type,
        String title,
        String content,
        boolean isPrivate
) {
    public static QnaResponse from(Qna qna) {
        return new QnaResponse(
                qna.getMember(),
                qna.getProduct(),
                qna.getType(),
                qna.getTitle(),
                qna.getContent(),
                qna.isPrivate()
        );
    }
}
