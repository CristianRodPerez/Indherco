package com.indherco.postes.shared.exception;

import java.time.LocalDateTime;

public record ApiError(
    LocalDateTime timestamp,
    int status,
    String code,
    String message,
    String path,
    String correlationId
) {
}
