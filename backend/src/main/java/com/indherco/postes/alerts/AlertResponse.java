package com.indherco.postes.alerts;

import com.indherco.postes.shared.enums.AlertLevel;
import com.indherco.postes.shared.enums.AlertType;
import java.time.LocalDateTime;

public record AlertResponse(
    Long id,
    AlertType type,
    String message,
    AlertLevel level,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {
}
