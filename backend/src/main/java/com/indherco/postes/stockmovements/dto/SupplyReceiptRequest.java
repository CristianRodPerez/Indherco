package com.indherco.postes.stockmovements.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SupplyReceiptRequest(
    @NotNull Long supplyId,
    @NotNull @Min(1) Integer quantity,
    String observation
) {}
