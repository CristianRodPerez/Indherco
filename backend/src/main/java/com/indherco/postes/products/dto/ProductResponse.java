package com.indherco.postes.products.dto;

public record ProductResponse(
    Long id,
    String name,
    String type,
    String unitOfMeasure,
    Integer currentStock,
    Integer minimumStock,
    boolean active
) {
}
