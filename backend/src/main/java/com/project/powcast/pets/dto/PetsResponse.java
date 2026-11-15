package com.project.powcast.pets.dto;

import com.project.powcast.pets.entity.Pets;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetsResponse(
        String name,
        Pets.PetType petType,
        LocalDate birthday,
        String breedName,
        BigDecimal weightKg,
        String allergies
) {
    public static PetsResponse from(Pets pets) {
        return new PetsResponse(
                pets.getName(),
                pets.getPetType(),
                pets.getBirthday(),
                pets.getBreedName(),
                pets.getWeightKg(),
                pets.getAllergies()
        );
    }
}
