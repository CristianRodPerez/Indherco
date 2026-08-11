package com.indherco.postes.supplies;

import com.indherco.postes.supplies.dto.SupplyResponse;
import org.springframework.stereotype.Component;

@Component
public class SupplyMapper {

    public SupplyResponse toResponse(Supply supply) {
        return new SupplyResponse(
            supply.getId(),
            supply.getName(),
            supply.getCategory(),
            supply.getUnitOfMeasure(),
            supply.getCurrentStock(),
            supply.getMinimumStock(),
            supply.isActive()
        );
    }
}
