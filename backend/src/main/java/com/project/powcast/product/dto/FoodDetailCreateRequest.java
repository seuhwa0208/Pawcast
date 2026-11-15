package com.project.powcast.product.dto;

import jakarta.validation.constraints.Size;

public record FoodDetailCreateRequest(

        @Size(max = 255, message = "식품 종류는 255자 이하여야 합니다.")
        String foodKind,

        @Size(max = 255, message = "식품 형태는 255자 이하여야 합니다.")
        String foodForm,

        @Size(max = 255, message = "주원료는 255자 이하여야 합니다.")
        String mainIngredient,

        @Size(max = 255, message = "생애 단계는 255자 이하여야 합니다.")
        String lifeStage,

        String ingredientDescription
) {
}