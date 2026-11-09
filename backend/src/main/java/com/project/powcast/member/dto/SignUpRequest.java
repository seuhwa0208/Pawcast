package com.project.powcast.member.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 6, max = 20, message = "아이디는 6~20자로 입력해주세요.")
    @Pattern(regexp = "^[a-z][a-z0-9]*$", message = "아이디는 영문 소문자로 시작하고, 영문 소문자와 숫자만 사용 가능합니다.")
    private String userId;

    @NotBlank(message = "이름을 입력해주세요.")
    private String username;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "^010\\d{8}$", message = "올바른 휴대전화 번호 형식이 아닙니다.")
    private String tel;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$", message = "영문과 숫자, 1개 이상의 특수문자를 사용해야 합니다.")
    private String passwordHash;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String checkPasswordHash;

    @AssertTrue(message = "이용약관에 동의해야 합니다.")
    private boolean isAgreedTerms;

    @AssertTrue(message = "개인정보이용에 동의해야 합니다.")
    private boolean isAgreedPrivacy;

    @AssertTrue(message = "전가금융거래에 동의해야 합니다.")
    private boolean isAgreedEfinance;

    private boolean isAgreedMarketing;
}
