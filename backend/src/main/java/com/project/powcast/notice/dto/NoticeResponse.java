package com.project.powcast.notice.dto;

import com.project.powcast.member.entity.Members;
import com.project.powcast.notice.entity.Notice;
import com.project.powcast.notice.entity.NoticeType;

import java.time.LocalDateTime;

public record NoticeResponse(

        Members members,
        NoticeType noticeType,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isPinned

) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getMember(),
                notice.getNoticeType(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                notice.isPinned()
        );
    }
}
