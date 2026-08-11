package com.indherco.postes.audit;

import com.indherco.postes.auth.security.SecurityUser;
import com.indherco.postes.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String module, String action, String entity, Long entityId, String previousDetail, String newDetail, String reason) {
        AuditLog log = new AuditLog();
        fillUser(log);
        fillRequest(log);
        log.setModule(module);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setPreviousDetail(previousDetail);
        log.setNewDetail(newDetail);
        log.setReason(reason);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> findLatest() {
        return auditLogRepository.findTop100ByOrderByOccurredAtDesc().stream().map(this::toResponse).toList();
    }

    private void fillUser(AuditLog log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            return;
        }
        log.setUserId(securityUser.getUser().getId());
        log.setUsername(securityUser.getUsername());
    }

    private void fillRequest(AuditLog log) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        log.setCorrelationId(correlationId == null ? null : correlationId.toString());
    }

    private AuditResponse toResponse(AuditLog log) {
        return new AuditResponse(
            log.getId(),
            log.getUserId(),
            log.getUsername(),
            log.getModule(),
            log.getAction(),
            log.getEntity(),
            log.getEntityId(),
            log.getOccurredAt(),
            log.getIp(),
            log.getUserAgent(),
            log.getCorrelationId(),
            log.getPreviousDetail(),
            log.getNewDetail(),
            log.getReason()
        );
    }
}
