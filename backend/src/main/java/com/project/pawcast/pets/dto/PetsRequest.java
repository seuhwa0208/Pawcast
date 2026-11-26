package com.project.pawcast.pets.dto;

import com.project.pawcast.pets.entity.Pets;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetsRequest(

        @NotBlank(message = "펫 이름을 입력해 주세요.")
        @Size(max = 20, message = "펫 이름은 최대 20자 까지 가능합니다.")
        String name,

        @NotNull(message = "펫 타입을 입력해 주세요.")
        Pets.PetType petType,

        LocalDate birthday,

        String breedName,

        @DecimalMin(value = "0.01", message = "체중은 0.01kg 이하로는 입력이 불가능합니다.")
        @Digits(integer = 3, fraction = 2, message = "체중은 최대 3자리, 소수점 2자리까지 표시가 가능합니다.")
        BigDecimal weightKg,

        String lifeStage,

        String allergies
) {
}
