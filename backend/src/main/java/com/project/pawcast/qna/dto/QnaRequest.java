package com.project.pawcast.qna.dto;

import com.project.pawcast.qna.entity.QnaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaRequest(

        Long productId,

        QnaType type,

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 100, message = "제목은 최대 100자까지 가능합니다.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        String content,

        Boolean isPrivate
) {
}
