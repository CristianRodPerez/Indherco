package com.indherco.postes.officeinventory.dto;

public record OfficeInventoryItemResponse(
    Long id,
    String name,
    String category,
    String unitOfMeasure,
    Integer currentStock,
    Integer minimumStock,
    boolean active
) {
}
