package com.indherco.postes.dailyclosing;

import com.indherco.postes.shared.enums.DailyClosingStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyClosingResponse(
    Long id,
    LocalDate closingDate,
    DailyClosingStatus status,
    String closedBy,
    String reopenedBy,
    Integer totalProduction,
    Integer totalDispatch,
    Integer totalConsumption,
    String observation,
    LocalDateTime closedAt,
    LocalDateTime reopenedAt,
    String reopenReason
) {
}
