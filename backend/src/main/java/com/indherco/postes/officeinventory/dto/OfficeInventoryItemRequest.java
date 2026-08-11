package com.indherco.postes.officeinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record OfficeInventoryItemRequest(
    @NotBlank String name,
    String category,
    @NotBlank String unitOfMeasure,
    @PositiveOrZero Integer currentStock,
    @PositiveOrZero Integer minimumStock
) {
}
