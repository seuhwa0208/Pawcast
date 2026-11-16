package com.project.powcast.qna.dto;

import com.project.powcast.member.entity.Members;
import com.project.powcast.notice.dto.NoticeResponse;
import com.project.powcast.notice.entity.Notice;
import com.project.powcast.product.entity.Product;
import com.project.powcast.qna.entity.Qna;
import com.project.powcast.qna.entity.QnaType;

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
