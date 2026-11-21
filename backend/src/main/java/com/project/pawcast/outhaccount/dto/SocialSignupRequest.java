package com.project.pawcast.outhaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialSignupRequest(
        @NotBlank String idToken,
        @NotBlank String name,
        @NotBlank String tel,
        @NotNull Boolean agreedTerms,
        @NotNull Boolean agreedPrivacy,
        @NotNull Boolean agreedEfinance,
        boolean agreedMarketing
) {
}
