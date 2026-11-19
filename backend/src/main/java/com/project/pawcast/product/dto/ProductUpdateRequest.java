package com.project.pawcast.product.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record ProductUpdateRequest(
    @Size(min=1, max=200) String name,
    @Size(max=100) String brand,
    @Positive Long categoryId,
    @Size(max=10000) String description,
    @Size(max=20) @Valid List<@NotNull ImageCreateRequest> images,
    @Valid FoodDetailCreateRequest foodDetail,
    @Valid DeviceDetailCreateRequest deviceDetail
) {}
