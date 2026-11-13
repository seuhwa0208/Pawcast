package com.project.powcast.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "배송지 이름을 작성해 주세요.")
        @Size(max = 50, message = "배송지 이름은 최대 50자 까지 가능합니다.")
        String label,

        @NotBlank(message = "받는 사람을 입력해 주세요.")
        @Size(max = 50, message = "받는 사람은 최대 50자 까지 가능합니다.")
        String recipient,

        String tel,

        @NotBlank(message = "우편번호를 입력해 주세요.")
        @Size(min =5, max = 5, message = "우편번호는 5자 입니다.")
        String zipcode,

        @NotBlank(message = "주소를 입력해주세요.")
        @Size(max = 200, message = "주소는 최대 200자 까지 가능합니다.")
        String address1,

        @NotBlank(message = "상세 주소를 입력해 주세요.")
        @Size(max = 200, message = "상세 주소는 최대 200자 까지 가능합니다.")
        String address2,

        boolean isDefault
) {
}
