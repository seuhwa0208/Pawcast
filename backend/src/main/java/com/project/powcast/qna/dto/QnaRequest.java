package com.project.powcast.qna.dto;

import com.project.powcast.product.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaRequest(

        Long productId,

        @NotBlank
        @Size(max = 100)
        String title,

        @NotBlank
        String content,

        Boolean isPrivate
) {
}
