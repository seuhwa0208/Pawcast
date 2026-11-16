package com.project.powcast.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductSearchRequest(
        @Size(max = 200, message = "검색어는 200자 이하여야 합니다.")
        String keyword,

        @Pattern(
                regexp = "FOOD|DEVICE",
                message = "상품 종류는 FOOD 또는 DEVICE여야 합니다."
        )
        String productType,

        @Positive(message = "카테고리 ID는 양수여야 합니다.")
        Long categoryId,

        @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
        String brand,

        String ingredient,

        String foodKind,

        String foodForm,

        String lifeStage,

        @DecimalMin(value = "0", message = "최소 가격은 0 이상이어야 합니다.")
        BigDecimal minPrice,

        @DecimalMin(value = "0", message = "최대 가격은 0 이상이어야 합니다.")
        BigDecimal maxPrice,

        @DecimalMin(value = "0", message = "평점은 0 이상이어야 합니다.")
        @DecimalMax(value = "5",message = "평점은 5 이하여야 합니다.")
        @Digits(integer = 1, fraction = 1, message = "평점은 소수점 한 자리까지 입력할 수 있습니다.")
        BigDecimal minRating,

        Boolean inStockOnly,

        @Pattern(regexp = "POPULAR|WISH_DESC|LATEST|PRICE_ASC|PRICE_DESC"
                + "|DISCOUNT_DESC|SALES_DESC|REVIEW_DESC|RATING_DESC",
                message = "지원하지 않는 정렬 방식입니다.")
        String sortType

) {
    public ProductSearchRequest {
        keyword = normalize(keyword);
        productType = normalize(productType);
        brand = normalize(brand);
        ingredient = normalize(ingredient);
        foodKind = normalize(foodKind);
        foodForm = normalize(foodForm);
        lifeStage = normalize(lifeStage);
        sortType = normalize(sortType);

        if (inStockOnly == null) {
            inStockOnly = false;
        }

        if (sortType == null) {
            sortType = "LATEST";
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}