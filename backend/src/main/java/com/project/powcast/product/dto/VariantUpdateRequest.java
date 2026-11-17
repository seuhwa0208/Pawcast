package com.project.powcast.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VariantUpdateRequest(

        @NotBlank(message = "SKU 코드를 입력해주세요.")
        @Size(max = 255, message = "SKU 코드는 255자 이하여야 합니다.")
        String skuCode,

        @NotBlank(message = "옵션 키를 입력해주세요.")
        @Size(max = 150, message = "옵션 키는 150자 이하여야 합니다.")
        String optionKey,

        @NotBlank(message = "옵션명을 입력해주세요.")
        @Size(max = 150, message = "옵션명은 150자 이하여야 합니다.")
        String optionLabel,

        @Size(max = 50, message = "색상은 50자 이하여야 합니다.")
        String color,

        @Positive(message = "무게는 0보다 커야 합니다.")
        Integer weightG,

        @Positive(message = "용량은 0보다 커야 합니다.")
        Integer volumeMl,

        @Positive(message = "수용 용량은 0보다 커야 합니다.")
        Integer capacityMl,

        @Positive(message = "높이는 0보다 커야 합니다.")
        Integer heightCm,

        @NotNull(message = "판매 가격을 입력해주세요.")
        @DecimalMin(value = "0", message = "판매 가격은 0 이상이어야 합니다.")
        @Digits(integer = 12, fraction = 0, message = "판매 가격은 최대 12자리의 정수여야 합니다.")
        BigDecimal price,

        @NotNull(message = "원가를 입력해주세요.")
        @DecimalMin(value = "0", message = "원가는 0 이상이어야 합니다.")
        @Digits(integer = 12, fraction = 0, message = "원가는 최대 12자리의 정수여야 합니다.")
        BigDecimal costPrice,

        @NotNull com.project.powcast.product.entity.ProductVariantStatus status

) {
    public VariantUpdateRequest {
        if (skuCode != null) {
            skuCode = skuCode.strip();
        }
    }
}
