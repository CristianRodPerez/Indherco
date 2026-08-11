package com.indherco.postes.stockmovements.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record DispatchRequest(
    LocalDate movementDate,
    @NotNull Long productId,
    @NotNull @Positive Integer quantity,
    String clientOrDestination,
    String transport,
    String guideNumber,
    String observation
) {
}
