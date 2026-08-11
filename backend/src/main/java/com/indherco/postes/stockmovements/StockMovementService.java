package com.indherco.postes.stockmovements;

import com.indherco.postes.alerts.AlertService;
import com.indherco.postes.audit.AuditService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.dailyclosing.DailyClosingService;
import com.indherco.postes.idempotency.IdempotencyService;
import com.indherco.postes.products.Product;
import com.indherco.postes.products.ProductRepository;
import com.indherco.postes.shared.enums.AlertLevel;
import com.indherco.postes.shared.enums.AlertType;
import com.indherco.postes.shared.enums.EntityType;
import com.indherco.postes.shared.enums.MovementStatus;
import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.stockmovements.dto.CancelMovementRequest;
import com.indherco.postes.stockmovements.dto.ConsumptionRequest;
import com.indherco.postes.stockmovements.dto.DispatchRequest;
import com.indherco.postes.stockmovements.dto.MovementResponse;
import com.indherco.postes.stockmovements.dto.ProductionRequest;
import com.indherco.postes.supplies.Supply;
import com.indherco.postes.supplies.SupplyRepository;
import com.indherco.postes.users.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final CurrentUserService currentUserService;
    private final StockMovementMapper movementMapper;
    private final AlertService alertService;
    private final AuditService auditService;
    private final DailyClosingService dailyClosingService;
    private final IdempotencyService idempotencyService;

    public StockMovementService(
        StockMovementRepository movementRepository,
        ProductRepository productRepository,
        SupplyRepository supplyRepository,
        CurrentUserService currentUserService,
        StockMovementMapper movementMapper,
        AlertService alertService,
        AuditService auditService,
        DailyClosingService dailyClosingService,
        IdempotencyService idempotencyService
    ) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
        this.currentUserService = currentUserService;
        this.movementMapper = movementMapper;
        this.alertService = alertService;
        this.auditService = auditService;
        this.dailyClosingService = dailyClosingService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public MovementResponse registerProduction(ProductionRequest request) {
        User user = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin() && !user.isCanRegisterProduction()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este usuario no puede registrar produccion.");
        }
        var previousResult = idempotencyService.findExistingResultId("StockMovement");
        if (previousResult.isPresent()) {
            return movementRepository.findById(previousResult.get()).map(movementMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_RESULT_NOT_FOUND", "No se encontro el resultado anterior."));
        }
        ensureDateIsOpen(request.movementDate());

        Product product = productRepository.findByIdForUpdate(request.productId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
        ensureActive(product.isActive(), "Producto inactivo.");

        Integer previous = product.getCurrentStock();
        Integer next = previous + request.quantity();
        product.setCurrentStock(next);

        StockMovement movement = baseMovement(user, MovementType.PRODUCCION, EntityType.PRODUCTO, request.quantity(), previous, next);
        movement.setProduct(product);
        movement.setUnitOfMeasure(product.getUnitOfMeasure());
        movement.setMovementDate(request.movementDate());
        movement.setRejectedQuantity(request.rejectedQuantity());
        movement.setShift(request.shift());
        movement.setObservation(request.observation());
        StockMovement saved = movementRepository.save(movement);
        auditService.record("MOVIMIENTOS", "CREATE_PRODUCTION", "StockMovement", saved.getId(), null, auditDetail(saved), null);
        idempotencyService.recordResult("StockMovement", saved.getId());
        return movementMapper.toResponse(saved);
    }

    @Transactional
    public MovementResponse registerDispatch(DispatchRequest request) {
        User user = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin() && !user.isCanRegisterDispatch()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este usuario no puede registrar despachos.");
        }
        var previousResult = idempotencyService.findExistingResultId("StockMovement");
        if (previousResult.isPresent()) {
            return movementRepository.findById(previousResult.get()).map(movementMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_RESULT_NOT_FOUND", "No se encontro el resultado anterior."));
        }
        ensureDateIsOpen(request.movementDate());

        Product product = productRepository.findByIdForUpdate(request.productId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
        ensureActive(product.isActive(), "Producto inactivo.");
        ensureEnoughStock(product.getCurrentStock(), request.quantity(), AlertType.DESPACHO_SIN_STOCK, "No hay stock suficiente para despachar esta cantidad.");

        Integer previous = product.getCurrentStock();
        Integer next = previous - request.quantity();
        product.setCurrentStock(next);

        StockMovement movement = baseMovement(user, MovementType.DESPACHO, EntityType.PRODUCTO, request.quantity(), previous, next);
        movement.setProduct(product);
        movement.setUnitOfMeasure(product.getUnitOfMeasure());
        movement.setMovementDate(request.movementDate());
        movement.setClientOrDestination(request.clientOrDestination());
        movement.setTransport(request.transport());
        movement.setGuideNumber(request.guideNumber());
        movement.setObservation(request.observation());
        MovementResponse response = movementMapper.toResponse(movementRepository.save(movement));
        auditService.record("MOVIMIENTOS", "CREATE_DISPATCH", "StockMovement", movement.getId(), null, auditDetail(movement), null);
        idempotencyService.recordResult("StockMovement", movement.getId());
        alertService.evaluateProductStock(product);
        return response;
    }

    @Transactional
    public MovementResponse registerConsumption(ConsumptionRequest request) {
        User user = currentUserService.getCurrentUser();
        if (!currentUserService.isAdmin() && !user.isCanRegisterConsumption()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Este usuario no puede registrar consumos.");
        }
        var previousResult = idempotencyService.findExistingResultId("StockMovement");
        if (previousResult.isPresent()) {
            return movementRepository.findById(previousResult.get()).map(movementMapper::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_RESULT_NOT_FOUND", "No se encontro el resultado anterior."));
        }
        ensureDateIsOpen(request.movementDate());

        Supply supply = supplyRepository.findByIdForUpdate(request.supplyId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insumo no encontrado."));
        ensureActive(supply.isActive(), "Insumo inactivo.");
        ensureEnoughStock(supply.getCurrentStock(), request.quantity(), AlertType.CONSUMO_SIN_STOCK, "No hay stock suficiente para consumir esta cantidad.");

        Integer previous = supply.getCurrentStock();
        Integer next = previous - request.quantity();
        supply.setCurrentStock(next);

        StockMovement movement = baseMovement(user, MovementType.CONSUMO, EntityType.INSUMO, request.quantity(), previous, next);
        movement.setSupply(supply);
        movement.setUnitOfMeasure(supply.getUnitOfMeasure());
        movement.setMovementDate(request.movementDate());
        movement.setProcessArea(request.processArea());
        movement.setObservation(request.observation());
        MovementResponse response = movementMapper.toResponse(movementRepository.save(movement));
        auditService.record("MOVIMIENTOS", "CREATE_CONSUMPTION", "StockMovement", movement.getId(), null, auditDetail(movement), null);
        idempotencyService.recordResult("StockMovement", movement.getId());
        alertService.evaluateSupplyStock(supply);
        return response;
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findAll(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return movementRepository.findByMovementDateBetweenOrderByRegisteredAtDesc(start, end)
            .stream().map(movementMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findMine() {
        User user = currentUserService.getCurrentUser();
        return movementRepository.findByRegisteredByOrderByRegisteredAtDesc(user)
            .stream().map(movementMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findToday() {
        return movementRepository.findByMovementDateOrderByRegisteredAtDesc(LocalDate.now())
            .stream().map(movementMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findLatest() {
        return movementRepository.findTop10ByOrderByRegisteredAtDesc().stream().map(movementMapper::toResponse).toList();
    }

    @Transactional
    public MovementResponse cancelMovement(Long id, CancelMovementRequest request) {
        User user = currentUserService.getCurrentUser();
        StockMovement original = movementRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movimiento no encontrado."));
        if (original.getStatus() == MovementStatus.ANULADO) {
            throw new ApiException(HttpStatus.CONFLICT, "MOVIMIENTO_YA_ANULADO", "El movimiento ya fue anulado.");
        }
        if (original.getMovementType() == MovementType.ANULACION) {
            throw new ApiException(HttpStatus.CONFLICT, "ANULACION_NO_ANULABLE", "No se puede anular un movimiento de anulacion.");
        }
        ensureDateIsOpen(original.getMovementDate());

        StockMovement reversal = reverseMovement(original, user, request.reason());
        StockMovement savedReversal = movementRepository.save(reversal);

        original.setStatus(MovementStatus.ANULADO);
        original.setCancelledBy(user);
        original.setCancelledAt(LocalDateTime.now());
        original.setCancellationReason(request.reason());
        original.setReversalMovementId(savedReversal.getId());

        auditService.record("MOVIMIENTOS", "CANCEL", "StockMovement", original.getId(), null, "reversalMovementId=" + savedReversal.getId(), request.reason());
        return movementMapper.toResponse(original);
    }

    private StockMovement baseMovement(User user, MovementType type, EntityType entityType, Integer quantity, Integer previous, Integer next) {
        StockMovement movement = new StockMovement();
        movement.setRegisteredBy(user);
        movement.setMovementType(type);
        movement.setEntityType(entityType);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previous);
        movement.setNewStock(next);
        return movement;
    }

    private StockMovement reverseMovement(StockMovement original, User user, String reason) {
        if (original.getEntityType() == EntityType.PRODUCTO) {
            Product product = productRepository.findByIdForUpdate(original.getProduct().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
            Integer previous = product.getCurrentStock();
            Integer next = original.getMovementType() == MovementType.PRODUCCION
                ? previous - original.getQuantity()
                : previous + original.getQuantity();
            if (next < 0) {
                throw new ApiException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE", "No hay stock suficiente para anular este movimiento.");
            }
            product.setCurrentStock(next);
            StockMovement reversal = baseMovement(user, MovementType.ANULACION, EntityType.PRODUCTO, original.getQuantity(), previous, next);
            reversal.setProduct(product);
            reversal.setUnitOfMeasure(original.getUnitOfMeasure());
            reversal.setMovementDate(LocalDate.now());
            reversal.setObservation("Anulacion movimiento #" + original.getId() + ": " + reason);
            return reversal;
        }

        Supply supply = supplyRepository.findByIdForUpdate(original.getSupply().getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insumo no encontrado."));
        Integer previous = supply.getCurrentStock();
        Integer next = previous + original.getQuantity();
        supply.setCurrentStock(next);
        StockMovement reversal = baseMovement(user, MovementType.ANULACION, EntityType.INSUMO, original.getQuantity(), previous, next);
        reversal.setSupply(supply);
        reversal.setUnitOfMeasure(original.getUnitOfMeasure());
        reversal.setMovementDate(LocalDate.now());
        reversal.setObservation("Anulacion movimiento #" + original.getId() + ": " + reason);
        return reversal;
    }

    private void ensureActive(boolean active, String message) {
        if (!active) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void ensureEnoughStock(Integer currentStock, Integer quantity, AlertType alertType, String message) {
        if (currentStock < quantity) {
            alertService.create(alertType, AlertLevel.CRITICA, message);
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void ensureDateIsOpen(LocalDate movementDate) {
        LocalDate date = movementDate == null ? LocalDate.now() : movementDate;
        if (dailyClosingService.isClosed(date)) {
            throw new ApiException(HttpStatus.CONFLICT, "DIA_CERRADO", "El dia ya esta cerrado. Reabra el dia antes de registrar movimientos.");
        }
    }

    private String auditDetail(StockMovement movement) {
        return "type=" + movement.getMovementType()
            + ", quantity=" + movement.getQuantity()
            + ", previousStock=" + movement.getPreviousStock()
            + ", newStock=" + movement.getNewStock();
    }
}
