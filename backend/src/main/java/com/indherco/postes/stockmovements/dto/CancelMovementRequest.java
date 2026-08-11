package com.indherco.postes.stockmovements.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelMovementRequest(
    @NotBlank String reason
) {
}
