package com.indherco.postes.stockmovements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ProductionRequest(
    LocalDate movementDate,
    @NotNull Long productId,
    @NotNull @Positive Integer quantity,
    Integer rejectedQuantity,
    String shift,
    String observation
) {
}
