package com.indherco.postes.dailyclosing;

import java.time.LocalDate;

public record DailyClosingRequest(
    LocalDate closingDate,
    String observation
) {
}
