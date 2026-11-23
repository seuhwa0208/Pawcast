package com.project.pawcast.order.event;

import java.math.BigDecimal;

/** 결제 커밋 뒤 주문번호 안내 메일을 발송하기 위한 이벤트다. */
public record OrderPaidEvent(
        String recipientEmail,
        String recipientName,
        String orderNumber,
        BigDecimal finalAmount
) {
}
