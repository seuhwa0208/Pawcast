package com.project.pawcast.pets.dto;

import com.project.pawcast.pets.entity.Pets;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetsResponse(
        Long id,
        String name,
        Pets.PetType petType,
        LocalDate birthday,
        String breedName,
        BigDecimal weightKg,
        String lifeStage,
        String allergies
) {
    public static PetsResponse from(Pets pets) {
        return new PetsResponse(
                pets.getId(),
                pets.getName(),
                pets.getPetType(),
                pets.getBirthday(),
                pets.getBreedName(),
                pets.getWeightKg(),
                pets.getLifeStage(),
                pets.getAllergies()
        );
    }
}
