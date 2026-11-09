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
    REQUIRED_EFINANCE_AGREEMENT(HttpStatus.BAD_REQUEST,"전자금융거래 이용약관에 동의해주세요."),

    CHECK_PASSWORD_ERROR(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");


    private final HttpStatus status;
    private final String message;
}
