package com.project.powcast.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        String password,

        @NotBlank
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$", message = "영문과 숫자, 1개 이상의 특수문자를 사용해야 합니다.")
        String newPassword,

        @NotBlank
        String newPasswordCheck
) {

}
