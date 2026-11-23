package com.project.pawcast.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CartMergeRequest(
        @NotNull(message = "장바구니 병합 요청 ID가 필요합니다.")
        UUID mergeId,

        @NotEmpty(message = "병합할 장바구니 상품을 한 개 이상 보내 주세요.")
        List<@NotNull(message = "장바구니 상품 정보가 비어 있습니다.")
        @Valid CartAddRequest> items
) {
}