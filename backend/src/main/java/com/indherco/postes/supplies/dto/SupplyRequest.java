package com.indherco.postes.supplies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SupplyRequest(
    @NotBlank String name,
    String category,
    @NotBlank String unitOfMeasure,
    @PositiveOrZero Integer currentStock,
    @PositiveOrZero Integer minimumStock
) {
}
