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
    EXPIRED_EMAIL_CODE(HttpStatus.GONE,"이메일 인증번호가 만료되었습니다."),
    EMAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"이메일 발송에 실패했습니다.");


    private final HttpStatus status;
    private final String message;
}
