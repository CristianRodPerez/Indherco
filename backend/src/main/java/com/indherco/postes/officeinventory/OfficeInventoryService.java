package com.indherco.postes.officeinventory;

import com.indherco.postes.audit.AuditService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.officeinventory.dto.OfficeInventoryItemRequest;
import com.indherco.postes.officeinventory.dto.OfficeInventoryItemResponse;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementRequest;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementResponse;
import com.indherco.postes.shared.enums.OfficeInventoryMovementType;
import com.indherco.postes.shared.exception.ApiException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles office inventory items and movements without mixing them with factory stock.
 */
@Service
public class OfficeInventoryService {

    private final OfficeInventoryItemRepository itemRepository;
    private final OfficeInventoryMovementRepository movementRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public OfficeInventoryService(
        OfficeInventoryItemRepository itemRepository,
        OfficeInventoryMovementRepository movementRepository,
        CurrentUserService currentUserService,
        AuditService auditService
    ) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public List<OfficeInventoryItemResponse> findItems(boolean activeOnly) {
        List<OfficeInventoryItem> items = activeOnly ? itemRepository.findByActiveTrueOrderByNameAsc() : itemRepository.findAll();
        return items.stream().map(this::toItemResponse).toList();
    }

    @Transactional
    public OfficeInventoryItemResponse createItem(OfficeInventoryItemRequest request) {
        OfficeInventoryItem item = new OfficeInventoryItem();
        applyItem(item, request, true);
        OfficeInventoryItem saved = itemRepository.save(item);
        auditService.record("INVENTARIO_OFICINA", "CREATE_ITEM", "OfficeInventoryItem", saved.getId(), null, "name=" + saved.getName(), null);
        return toItemResponse(saved);
    }

    @Transactional
    public OfficeInventoryItemResponse updateItem(Long id, OfficeInventoryItemRequest request) {
        OfficeInventoryItem item = itemRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item de inventario no encontrado."));
        applyItem(item, request, false);
        auditService.record("INVENTARIO_OFICINA", "UPDATE_ITEM", "OfficeInventoryItem", item.getId(), null, "name=" + item.getName(), null);
        return toItemResponse(item);
    }

    @Transactional
    public OfficeInventoryItemResponse setItemStatus(Long id, boolean active) {
        OfficeInventoryItem item = itemRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item de inventario no encontrado."));
        item.setActive(active);
        auditService.record("INVENTARIO_OFICINA", "STATUS_CHANGE", "OfficeInventoryItem", item.getId(), null, "active=" + active, null);
        return toItemResponse(item);
    }

    @Transactional
    public OfficeInventoryMovementResponse registerMovement(OfficeInventoryMovementRequest request) {
        OfficeInventoryItem item = itemRepository.findByIdForUpdate(request.itemId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item de inventario no encontrado."));
        if (!item.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El item esta inactivo.");
        }

        Integer previous = item.getCurrentStock();
        Integer next = request.movementType() == OfficeInventoryMovementType.ENTRADA
            ? previous + request.quantity()
            : previous - request.quantity();

        if (next < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay stock suficiente para consumir esta cantidad.");
        }

        item.setCurrentStock(next);

        OfficeInventoryMovement movement = new OfficeInventoryMovement();
        movement.setItem(item);
        movement.setMovementType(request.movementType());
        movement.setQuantity(request.quantity());
        movement.setPreviousStock(previous);
        movement.setNewStock(next);
        movement.setObservation(request.observation());
        movement.setMovementDate(request.movementDate());
        movement.setRegisteredBy(currentUserService.getCurrentUser());
        OfficeInventoryMovement saved = movementRepository.save(movement);
        auditService.record("INVENTARIO_OFICINA", "CREATE_MOVEMENT", "OfficeInventoryMovement", saved.getId(), null, auditDetail(saved), request.observation());
        return toMovementResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OfficeInventoryMovementResponse> findMovements(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        return movementRepository.findByMovementDateBetweenOrderByRegisteredAtDesc(start, end)
            .stream().map(this::toMovementResponse).toList();
    }

    private void applyItem(OfficeInventoryItem item, OfficeInventoryItemRequest request, boolean allowInitialStock) {
        item.setName(request.name());
        item.setCategory(request.category());
        item.setUnitOfMeasure(request.unitOfMeasure());
        item.setMinimumStock(request.minimumStock());
        if (allowInitialStock) {
            item.setCurrentStock(request.currentStock() == null ? 0 : request.currentStock());
        }
    }

    private OfficeInventoryItemResponse toItemResponse(OfficeInventoryItem item) {
        return new OfficeInventoryItemResponse(
            item.getId(),
            item.getName(),
            item.getCategory(),
            item.getUnitOfMeasure(),
            item.getCurrentStock(),
            item.getMinimumStock(),
            item.isActive()
        );
    }

    private OfficeInventoryMovementResponse toMovementResponse(OfficeInventoryMovement movement) {
        return new OfficeInventoryMovementResponse(
            movement.getId(),
            movement.getMovementType(),
            movement.getItem().getId(),
            movement.getItem().getName(),
            movement.getQuantity(),
            movement.getItem().getUnitOfMeasure(),
            movement.getPreviousStock(),
            movement.getNewStock(),
            movement.getObservation(),
            movement.getRegisteredBy().getName(),
            movement.getMovementDate(),
            movement.getRegisteredAt()
        );
    }

    private String auditDetail(OfficeInventoryMovement movement) {
        return "type=" + movement.getMovementType()
            + ", quantity=" + movement.getQuantity()
            + ", previousStock=" + movement.getPreviousStock()
            + ", newStock=" + movement.getNewStock();
    }
}
