package com.project.powcast.product.dto;
import jakarta.validation.constraints.*;
public record StockAdjustmentRequest(@NotNull Integer quantityChange, @NotBlank @Size(max=255) String reason) {}
