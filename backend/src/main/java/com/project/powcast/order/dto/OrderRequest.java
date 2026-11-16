package com.project.powcast.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest (
        @NotBlank(message = "받는사람 성함을 입력해 주세요.")
        @Size(max = 20, message = "이름은 최대 20자까지 가능합니다.")
        String recipientName,

        @Size(max = 20, message = "전화번호 입력은 최대 20자까지 가능합니다.")
        String recipientPhone,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Size(max = 100, message = "이메일은 최대 100자까지 가능합니다.")
        String recipientEmail,

        @NotBlank(message = "우편번호를 입력해 주세요.")
        @Size(min = 5, max = 5, message = "우편번호는 5자 입니다.")
        String deliveryZipcode,

        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 255)
        String deliveryAddress1,

        @Size(max = 255)
        String deliveryAddress2,

        @Size(max = 255)
        String deliveryMessage,

         List<OrderItemRequest> items
){
}
