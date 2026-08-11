package com.indherco.postes.idempotency;

import com.indherco.postes.auth.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class IdempotencyService {

    private static final String HEADER = "Idempotency-Key";

    private final IdempotencyRecordRepository repository;
    private final CurrentUserService currentUserService;

    public IdempotencyService(IdempotencyRecordRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    public Optional<Long> findExistingResultId(String resultType) {
        String key = currentKey();
        if (key == null) {
            return Optional.empty();
        }
        Long userId = currentUserService.getCurrentUser().getId();
        String endpoint = currentEndpoint();
        return repository.findByUserIdAndEndpointAndKey(userId, endpoint, key)
            .filter(record -> resultType.equals(record.getResultType()))
            .map(IdempotencyRecord::getResultId);
    }

    public void recordResult(String resultType, Long resultId) {
        String key = currentKey();
        if (key == null || resultId == null) {
            return;
        }
        Long userId = currentUserService.getCurrentUser().getId();
        String endpoint = currentEndpoint();
        if (repository.findByUserIdAndEndpointAndKey(userId, endpoint, key).isPresent()) {
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setKey(key);
        record.setEndpoint(endpoint);
        record.setUserId(userId);
        record.setStatus("COMPLETED");
        record.setResultType(resultType);
        record.setResultId(resultId);
        repository.save(record);
    }

    private String currentKey() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String key = request.getHeader(HEADER);
        return key == null || key.isBlank() ? null : key.trim();
    }

    private String currentEndpoint() {
        HttpServletRequest request = currentRequest();
        return request == null ? "unknown" : request.getMethod() + " " + request.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }
}
