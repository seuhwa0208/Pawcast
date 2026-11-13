package com.project.powcast.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
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
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않은 회원입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해주세요."),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "새 비밀번호 확인과 일치하지 않습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
    NOT_FOUND_ADDRESS(HttpStatus.NOT_FOUND,"존재하지 않은 배송지 입니다."),
    REQUIRED_EFINANCE_AGREEMENT(HttpStatus.BAD_REQUEST,"전자금융거래 이용약관에 동의해주세요."),


    CHECK_PASSWORD_ERROR(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");


    private final HttpStatus status;
    private final String message;
}
