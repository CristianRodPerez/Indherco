package com.indherco.postes.stockmovements.dto;

import com.indherco.postes.shared.enums.EntityType;
import com.indherco.postes.shared.enums.MovementStatus;
import com.indherco.postes.shared.enums.MovementType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovementResponse(
    Long id,
    MovementType movementType,
    EntityType entityType,
    Long productId,
    Long supplyId,
    String itemName,
    Integer quantity,
    String unitOfMeasure,
    Integer previousStock,
    Integer newStock,
    String observation,
    String registeredBy,
    LocalDate movementDate,
    LocalDateTime registeredAt,
    MovementStatus status
) {
}
