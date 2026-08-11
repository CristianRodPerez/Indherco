package com.indherco.postes.stockmovements;

import com.indherco.postes.stockmovements.dto.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    public MovementResponse toResponse(StockMovement movement) {
        Long productId = movement.getProduct() == null ? null : movement.getProduct().getId();
        Long supplyId = movement.getSupply() == null ? null : movement.getSupply().getId();
        String itemName = movement.getProduct() != null
            ? movement.getProduct().getName()
            : movement.getSupply() == null ? "" : movement.getSupply().getName();

        return new MovementResponse(
            movement.getId(),
            movement.getMovementType(),
            movement.getEntityType(),
            productId,
            supplyId,
            itemName,
            movement.getQuantity(),
            movement.getUnitOfMeasure(),
            movement.getPreviousStock(),
            movement.getNewStock(),
            movement.getObservation(),
            movement.getRegisteredBy().getName(),
            movement.getMovementDate(),
            movement.getRegisteredAt(),
            movement.getStatus()
        );
    }
}
