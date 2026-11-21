package com.project.pawcast.refund.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundCancelReasonRequest(

        @NotBlank(message = "환불 사유를 입력해주세요.")
        String cancelReason
) {
}
