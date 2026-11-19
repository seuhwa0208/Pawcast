package com.project.pawcast.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest(

        @NotBlank(message = "이름을 입력해 주세요.")
        String name,

        String tel,

        boolean isAgreedMarketing
) {
}
