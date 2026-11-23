package com.project.pawcast.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(

        @NotBlank(message = "빈 칸을 입력해 주세요.")
        @Email(message = "이메일 형식으로 입력해 주세요.")
        String email
) {
}