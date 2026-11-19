package com.project.pawcast.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReviewImageCreateRequest(

        @NotBlank(message = "이미지 경로를 입력해주세요.")
        @Size(max = 255, message = "이미지 경로는 255자 이하여야 합니다.")
        String imagePath,

        @PositiveOrZero(message = "이미지 순서는 0 이상이어야 합니다.")
        int sortOrder

) {
}