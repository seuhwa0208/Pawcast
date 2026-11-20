package com.project.pawcast.notice.dto;

import com.project.pawcast.notice.entity.Notice;
import com.project.pawcast.notice.entity.NoticeType;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long noticeId,
        Long memberId,
        String memberName,
        NoticeType noticeType,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isPinned

) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getMember() == null ? null : notice.getMember().getId(),
                notice.getMember() == null ? null : notice.getMember().getName(),
                notice.getNoticeType(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                notice.isPinned()
        );
    }
}
