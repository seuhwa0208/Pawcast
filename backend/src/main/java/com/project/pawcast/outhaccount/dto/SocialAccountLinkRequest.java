package com.project.pawcast.outhaccount.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialAccountLinkRequest(
        @NotBlank String idToken,
        @NotBlank String password
) {
}
