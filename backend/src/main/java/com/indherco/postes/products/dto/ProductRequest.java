package com.indherco.postes.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
    @NotBlank String name,
    String type,
    @NotBlank String unitOfMeasure,
    @PositiveOrZero Integer currentStock,
    @PositiveOrZero Integer minimumStock
) {
}
