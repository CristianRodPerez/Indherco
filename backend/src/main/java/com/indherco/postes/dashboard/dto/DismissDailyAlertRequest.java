package com.indherco.postes.dashboard.dto;

import com.indherco.postes.shared.enums.AlertType;
import jakarta.validation.constraints.NotNull;

public record DismissDailyAlertRequest(
    @NotNull AlertType type
) {
}
