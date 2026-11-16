package com.project.powcast.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReviewCreateRequest(

        @NotNull(message = "주문 항목을 선택해주세요.")
        Long orderItemId,

        @NotNull(message = "평점을 입력해주세요.")
        @Min(value = 1, message = "평점은 최소 1점입니다.")
        @Max(value = 5, message = "평점은 최대 5점입니다.")
        Integer rating,

        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        String content,

        @Valid
        List<@NotNull ReviewImageCreateRequest> images

) {

    public ReviewCreateRequest {
        if (images == null) {
            images = List.of();
        }
    }
}