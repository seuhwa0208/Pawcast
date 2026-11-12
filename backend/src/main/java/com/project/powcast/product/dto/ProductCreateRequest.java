package com.project.powcast.product.dto;

import com.project.powcast.product.entity.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProductCreateRequest(

        @NotBlank(message = "상품명을 입력해주세요.")
        @Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
        String name,

        @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
        String brand,

        @NotNull(message = "상품 종류를 선택해주세요.")
        ProductType productType,

        @NotNull(message = "카테고리를 선택해주세요.")
        @Positive(message = "카테고리 ID는 양수여야 합니다.")
        Long categoryId,

        @Size(
                max = 10000,
                message = "상품 설명은 10,000자 이하여야 합니다."
        )
        String description,

        @NotEmpty(message = "옵션은 최소 한 개 이상 등록해야 합니다.")
        @Size(
                max = 100,
                message = "옵션은 최대 100개까지 등록할 수 있습니다."
        )
        @Valid
        List<@NotNull VariantCreateRequest> variants,

        @Size(
                max = 20,
                message = "이미지는 최대 20개까지 등록할 수 있습니다."
        )
        @Valid
        List<@NotNull ImageCreateRequest> images,

        @Valid
        FoodDetailCreateRequest foodDetail,

        @Valid
        DeviceDetailCreateRequest deviceDetail

) {

    public ProductCreateRequest {
        if (images == null) {
            images = List.of();
        }
    }
}