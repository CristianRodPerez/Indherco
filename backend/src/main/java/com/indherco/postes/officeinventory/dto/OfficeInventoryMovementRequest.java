package com.indherco.postes.officeinventory.dto;

import com.indherco.postes.shared.enums.OfficeInventoryMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record OfficeInventoryMovementRequest(
    LocalDate movementDate,
    @NotNull Long itemId,
    @NotNull OfficeInventoryMovementType movementType,
    @NotNull @Positive Integer quantity,
    String observation
) {
}
