package com.project.pawcast.payment.entity;

public enum PaymentsStatus {
    PENDING,   // 결제 대기
    PAID,      // 결제 완료
    CANCELLED, // 결제 취소 완료
    FAILED     // 결제 실패
}
