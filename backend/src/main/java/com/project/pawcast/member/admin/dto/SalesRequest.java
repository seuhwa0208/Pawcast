package com.project.pawcast.member.admin.dto;

import com.project.pawcast.member.admin.entity.SalesPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SalesRequest(

        @NotNull
        SalesPeriod period,

        @Min(2000)
        @Max(2100)
        Integer year,

        @Min(1)
        @Max(12)
        Integer month,

        @Min(1)
        @Max(4)
        Integer quarter,

        @Min(2000)
        @Max(2100)
        Integer startYear,

        @Min(2000)
        @Max(2100)
        Integer endYear
) {
}
