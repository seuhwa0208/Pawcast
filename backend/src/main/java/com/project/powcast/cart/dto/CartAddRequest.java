package com.project.powcast.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartAddRequest(
        @NotNull @Positive Long variantId,
        @NotNull @Positive Integer quantity
) {}
