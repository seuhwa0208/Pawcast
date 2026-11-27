package com.project.pawcast.order.event;

import com.project.pawcast.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidEmailListener {
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderNumber(OrderPaidEvent event) {
        try {
            emailService.sendEmail(
                    event.recipientEmail(),
                    "[PAWCAST] 주문이 완료되었습니다",
                    String.format(
                            "%s님, 결제가 완료되었습니다.\n\n주문번호: %s\n결제 금액: %s원\n\n비회원 주문 조회 시 주문번호와 주문 이메일이 필요합니다.",
                            event.recipientName(),
                            event.orderNumber(),
                            event.finalAmount().toPlainString()
                    )
            );
        } catch (RuntimeException exception) {
            // 결제와 주문은 이미 확정됐으므로 메일 문제는 운영 로그로만 남긴다.
            log.warn("주문 완료 메일 발송 실패: orderNumber={}", event.orderNumber(), exception);
        }
    }
}
