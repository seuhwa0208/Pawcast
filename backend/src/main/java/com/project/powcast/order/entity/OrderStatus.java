package com.project.powcast.order.entity;

public enum OrderStatus {
    //결제 대기중 -> 결제완료 → 배송준비 → 배송중 → 배송완료
    PENDING_PAYMENT,// 결제 대기중
    PAID,        // 결제완료
    PREPARING,   // 배송준비
    SHIPPING,    // 배송중
    DELIVERED,    // 배송완료
    CANCELLED	//이미 취소됨
}
