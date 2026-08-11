package com.indherco.postes.reports;

import com.indherco.postes.stockmovements.dto.MovementResponse;
import java.time.LocalDate;
import java.util.List;

public record ReportSummaryResponse(
    LocalDate from,
    LocalDate to,
    Integer totalProduction,
    Integer totalDispatch,
    Integer totalConsumption,
    List<MovementResponse> movements
) {
}
