package com.project.pawcast.notice.dto;

import com.project.pawcast.notice.entity.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
        // Enum 타입인 NoticeType은 문자열 전용 @NotBlank 대신 @NotNull로 검증해야 합니다.
        @NotNull(message = "공지사항 유형을 지정해주세요.")
        NoticeType noticeType,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 최대 100자 까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        boolean isPinned
) {
}