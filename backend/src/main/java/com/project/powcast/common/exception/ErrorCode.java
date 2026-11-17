package com.project.powcast.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    INVALID_PRODUCT_REQUEST(HttpStatus.BAD_REQUEST, "상품 요청값이 올바르지 않습니다."),
    INVALID_PRODUCT_DETAIL(HttpStatus.BAD_REQUEST, "상품 종류와 상세 정보가 일치하지 않습니다."),
    MULTIPLE_MAIN_IMAGES(HttpStatus.BAD_REQUEST, "대표 이미지는 최대 한 개만 지정할 수 있습니다."),
    DUPLICATE_SKU(HttpStatus.CONFLICT, "이미 사용 중이거나 중복된 SKU 코드입니다."),
    INVALID_VARIANT_FIELDS(HttpStatus.BAD_REQUEST, "상품 종류에 맞지 않는 옵션 규격입니다."),
    INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "최소 가격은 최대 가격보다 클 수 없습니다."),
    INVALID_STOCK_ADJUSTMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 재고 조정 수량입니다."),
    PRODUCT_DATA_CONFLICT(HttpStatus.CONFLICT, "상품 데이터가 다른 요청과 충돌합니다."),
    NOT_FOUND_CATEGORY(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    // 예외 상태, 메시지 추가
    DUPLICATE_USERID(HttpStatus.CONFLICT, "이미 사용 중인 아이디 입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일 입니다."),
    NOT_FOUND_EMAIL_VERIFICATION(HttpStatus.NOT_FOUND,"인증 정보를 찾을 수 없습니다."),
    NOT_VERIFIED_EMAIL(HttpStatus.NOT_FOUND,"인증된 이메일이 아닙니다."),
    EXPIRED_EMAIL_CODE(HttpStatus.GONE,"이메일 인증번호가 만료되었습니다."),
    EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"이메일 발송에 실패했습니다."),
    VERIFICATION_COMPLETED_EMAIL(HttpStatus.CONFLICT, "이미 인증된 이메일 입니다."),
    REQUIRED_PRIVACY_AGREEMENT(HttpStatus.BAD_REQUEST,"개인정보 이용약관에 동의해주세요."),
    REQUIRED_TERMS_AGREEMENT(HttpStatus.BAD_REQUEST,"서비스 이용약관에 동의해주세요."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,"토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    MEMBER_DELETED(HttpStatus.FORBIDDEN,"탈퇴한 회원입니다."),
    MEMBER_INACTIVE(HttpStatus.FORBIDDEN,"비활성화된 회원입니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND,"회원을 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해주세요."),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "새 비밀번호 확인과 일치하지 않습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
    NOT_FOUND_ADDRESS(HttpStatus.NOT_FOUND,"배송지를 찾을 수 없습니다."),
    NOT_FOUND_PETS(HttpStatus.NOT_FOUND,"반려동물 정보를 찾을 수 없습니다."),
    NOT_FOUND_WISHLIST(HttpStatus.NOT_FOUND,"찜 정보를 찾을 수 없습니다."),
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND,"상품 정보를 찾을 수 없습니다."),
    NOT_FOUND_NOTICE(HttpStatus.NOT_FOUND,"공지사항을 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,"권한이 없습니다."),
    NOT_FOUND_OPTION(HttpStatus.NOT_FOUND,"옵션을 찾을 수 없습니다."),
    NOT_FOUND_ORDER(HttpStatus.NOT_FOUND,"주문 정보를 찾을 수 없습니다."),
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST,"유효하지 않은 옵션입니다."),
    INVALID_ORDER_STATUS(HttpStatus.FORBIDDEN,"현재 주문 상태에서는 배송 정보를 수정할 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "상품의 재고가 부족합니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "이미 결제 정보가 존재합니다."),
    NOT_ORDER_OWNER(HttpStatus.FORBIDDEN, "본인의 주문만 결제할 수 있습니다."),
    INVALID_ORDER_NUMBER(HttpStatus.BAD_REQUEST, "주문번호가 일치하지 않습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_METHOD(HttpStatus.BAD_REQUEST, "지원하지 않는 결제 수단입니다."),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST,"유효하지 않은 결제 상태 입니다."),
    REQUIRED_EFINANCE_AGREEMENT(HttpStatus.BAD_REQUEST,"전자금융거래 이용약관에 동의해주세요."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST,"재고가 충분하지 않습니다."),
    NOT_FOUND_PRODUCT_VARIANT(HttpStatus.NOT_FOUND,"상품 속성을 찾을 수 없습니다."),
    NOT_FOUND_REFUND(HttpStatus.NOT_FOUND,"환불 정보를 찾을 수 없습니다."),
    NOT_FOUND_REVIEW(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    NOT_FOUND_ORDER_ITEM(HttpStatus.NOT_FOUND, "주문 항목을 찾을 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 리뷰를 작성한 주문 항목입니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST,"배송 완료된 주문만 리뷰를 작성할 수 있습니다."),
    PROFANITY_DETECTED(HttpStatus.BAD_REQUEST,"욕설이나 부적절한 표현이 포함되어 있습니다. 내용을 수정해주세요."),
    MODERATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"내용 검사를 완료하지 못했습니다. 잠시 후 다시 시도해주세요."),

    NOT_FOUND_CART_ITEM(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    INVALID_CART_QUANTITY(HttpStatus.BAD_REQUEST, "장바구니 수량은 1 이상이어야 합니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "현재 판매 중인 상품 옵션이 아닙니다."),

    NOT_FOUND_QNA(HttpStatus.NOT_FOUND,"질문 글을 찾을 수 없습니다."),
    NOT_FOUND_QNA_ANSWER(HttpStatus.NOT_FOUND,"답변 글을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN,"잘못된 접근 권한 입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST,"유효하지 않은 토큰 입니다."),
    DUPLICATE_MEMBER(HttpStatus.CONFLICT,"이미 존재하는 회원 입니다."),
    CHECK_PASSWORD_ERROR(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");


    private final HttpStatus status;
    private final String message;
}
