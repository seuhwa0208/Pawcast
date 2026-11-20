package com.project.pawcast.payment.dto;

import java.math.BigDecimal;
import java.util.List;

public record TossCancelResponse(
        List<CancelInfo> cancels
) {
    public record CancelInfo(
            String transactionKey,
            String cancelStatus,
            String cancelReason,
            BigDecimal cancelAmount,
            String canceledAt
    ) { }
}