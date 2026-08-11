package com.indherco.postes.officeinventory.dto;

import com.indherco.postes.shared.enums.OfficeInventoryMovementType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OfficeInventoryMovementResponse(
    Long id,
    OfficeInventoryMovementType movementType,
    Long itemId,
    String itemName,
    Integer quantity,
    String unitOfMeasure,
    Integer previousStock,
    Integer newStock,
    String observation,
    String registeredBy,
    LocalDate movementDate,
    LocalDateTime registeredAt
) {
}
