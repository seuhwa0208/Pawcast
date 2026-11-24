package com.project.pawcast.outhaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
        @NotBlank String idToken,
        @NotBlank
        @Size(min = 6, max = 20)
        @Pattern(regexp = "^[a-z][a-z0-9]*$")
        String userId,
        @NotBlank String name,
        @NotBlank String tel,
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$")
        String passwordHash,
        @NotBlank String checkPasswordHash,
        @NotNull Boolean agreedTerms,
        @NotNull Boolean agreedPrivacy,
        @NotNull Boolean agreedEfinance,
        boolean agreedMarketing
) {
}
