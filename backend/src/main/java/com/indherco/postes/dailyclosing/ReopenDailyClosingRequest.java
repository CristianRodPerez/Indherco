package com.indherco.postes.dailyclosing;

import jakarta.validation.constraints.NotBlank;

public record ReopenDailyClosingRequest(
    @NotBlank String reason
) {
}
