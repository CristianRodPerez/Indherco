package com.indherco.postes.audit;

import java.time.LocalDateTime;

public record AuditResponse(
    Long id,
    Long userId,
    String username,
    String module,
    String action,
    String entity,
    Long entityId,
    LocalDateTime occurredAt,
    String ip,
    String userAgent,
    String correlationId,
    String previousDetail,
    String newDetail,
    String reason
) {
}
