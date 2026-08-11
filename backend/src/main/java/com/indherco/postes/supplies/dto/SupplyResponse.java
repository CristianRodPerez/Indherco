package com.indherco.postes.supplies.dto;

public record SupplyResponse(
    Long id,
    String name,
    String category,
    String unitOfMeasure,
    Integer currentStock,
    Integer minimumStock,
    boolean active
) {
}
