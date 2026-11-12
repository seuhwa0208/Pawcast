package com.project.powcast.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceDetailCreateRequest(

        @NotBlank(message = "기기 종류를 입력해주세요.")
        @Size(max = 255, message = "기기 종류는 255자 이하여야 합니다.")
        String deviceKind,

        @Size(max = 255, message = "재질은 255자 이하여야 합니다.")
        String material,

        String featureDescription

) {
}