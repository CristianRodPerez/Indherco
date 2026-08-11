package com.indherco.postes.dailyclosing;

import com.indherco.postes.audit.AuditService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.idempotency.IdempotencyService;
import com.indherco.postes.shared.enums.DailyClosingStatus;
import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.stockmovements.StockMovement;
import com.indherco.postes.stockmovements.StockMovementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyClosingService {

    private final DailyClosingRepository closingRepository;
    private final StockMovementRepository movementRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    public DailyClosingService(
        DailyClosingRepository closingRepository,
        StockMovementRepository movementRepository,
        CurrentUserService currentUserService,
        AuditService auditService,
        IdempotencyService idempotencyService
    ) {
        this.closingRepository = closingRepository;
        this.movementRepository = movementRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public DailyClosingResponse closeDay(DailyClosingRequest request) {
        LocalDate date = request.closingDate() == null ? LocalDate.now() : request.closingDate();
        var previousResult = idempotencyService.findExistingResultId("DailyClosing");
        if (previousResult.isPresent()) {
            return closingRepository.findById(previousResult.get()).map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_RESULT_NOT_FOUND", "No se encontro el resultado anterior."));
        }
        List<StockMovement> movements = movementRepository.findByMovementDateOrderByRegisteredAtDesc(date);
        DailyClosing closing = closingRepository.findByClosingDate(date).orElseGet(DailyClosing::new);
        if (closing.getId() != null && closing.getStatus() == DailyClosingStatus.CERRADO) {
            throw new ApiException(HttpStatus.CONFLICT, "DIA_YA_CERRADO", "El dia ya fue cerrado.");
        }

        boolean newClosing = closing.getId() == null;
        applyClosingData(closing, date, movements, request);
        DailyClosing saved = closingRepository.save(closing);
        auditService.record("CIERRES", newClosing ? "CLOSE_DAY" : "RECLOSE_DAY", "DailyClosing", saved.getId(), null, "date=" + date, request.observation());
        idempotencyService.recordResult("DailyClosing", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public DailyClosingResponse reopenDay(LocalDate date, ReopenDailyClosingRequest request) {
        DailyClosing closing = closingRepository.findByClosingDate(date)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe cierre para esa fecha."));
        if (closing.getStatus() == DailyClosingStatus.REABIERTO) {
            throw new ApiException(HttpStatus.CONFLICT, "DIA_YA_REABIERTO", "El dia ya fue reabierto.");
        }
        closing.setStatus(DailyClosingStatus.REABIERTO);
        closing.setReopenedBy(currentUserService.getCurrentUser());
        closing.setReopenedAt(LocalDateTime.now());
        closing.setReopenReason(request.reason());
        auditService.record("CIERRES", "REOPEN_DAY", "DailyClosing", closing.getId(), null, "date=" + date, request.reason());
        return toResponse(closing);
    }

    @Transactional(readOnly = true)
    public List<DailyClosingResponse> findLatest() {
        return closingRepository.findTop60ByOrderByClosingDateDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean isClosed(LocalDate date) {
        return closingRepository.findByClosingDate(date)
            .map(closing -> closing.getStatus() == DailyClosingStatus.CERRADO)
            .orElse(false);
    }

    public DailyClosingResponse findByDate(LocalDate date) {
        return closingRepository.findByClosingDate(date)
            .map(this::toResponse)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No existe cierre para esa fecha."));
    }

    private Integer sum(List<StockMovement> movements, MovementType type) {
        return movements.stream()
            .filter(movement -> movement.getMovementType() == type)
            .map(StockMovement::getQuantity)
            .reduce(0, Integer::sum);
    }

    private void applyClosingData(DailyClosing closing, LocalDate date, List<StockMovement> movements, DailyClosingRequest request) {
        closing.setClosingDate(date);
        closing.setClosedBy(currentUserService.getCurrentUser());
        closing.setStatus(DailyClosingStatus.CERRADO);
        closing.setClosedAt(LocalDateTime.now());
        closing.setTotalProduction(sum(movements, MovementType.PRODUCCION));
        closing.setTotalDispatch(sum(movements, MovementType.DESPACHO));
        closing.setTotalConsumption(sum(movements, MovementType.CONSUMO));
        closing.setObservation(request.observation());
    }

    private DailyClosingResponse toResponse(DailyClosing closing) {
        return new DailyClosingResponse(
            closing.getId(),
            closing.getClosingDate(),
            closing.getStatus(),
            closing.getClosedBy().getName(),
            closing.getReopenedBy() == null ? null : closing.getReopenedBy().getName(),
            closing.getTotalProduction(),
            closing.getTotalDispatch(),
            closing.getTotalConsumption(),
            closing.getObservation(),
            closing.getClosedAt(),
            closing.getReopenedAt(),
            closing.getReopenReason()
        );
    }
}
