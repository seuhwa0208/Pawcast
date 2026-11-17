package com.project.powcast.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartQuantityUpdateRequest(
        @NotNull @Positive Integer quantity
) {}
