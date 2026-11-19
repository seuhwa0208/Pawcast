package com.project.pawcast.qnaanswer.dto;

import jakarta.validation.constraints.NotBlank;

public record QnaAnswerRequest(

        @NotBlank(message = "답변을 입력해 주세요.")
        String content
) {
}
