package com.indherco.postes.stockmovements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ConsumptionRequest(
    LocalDate movementDate,
    @NotNull Long supplyId,
    @NotNull @Positive Integer quantity,
    String processArea,
    String observation
) {
}
