package com.project.powcast.product.dto;

import jakarta.validation.constraints.*;

public record ImageCreateRequest(

        @NotBlank(message = "이미지 경로를 입력해주세요.")
        @Size(max = 255, message = "이미지 경로는 255자 이하여야 합니다.")
        String imagePath,

        boolean main,

        @PositiveOrZero(message = "이미지 순서는 0 이상이어야 합니다.")
        int sortOrder

) {
}