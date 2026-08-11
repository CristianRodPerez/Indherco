package com.indherco.postes.officeinventory;

import com.indherco.postes.officeinventory.dto.OfficeInventoryItemRequest;
import com.indherco.postes.officeinventory.dto.OfficeInventoryItemResponse;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementRequest;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementResponse;
import com.indherco.postes.users.dto.StatusRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/office-inventory")
public class OfficeInventoryController {

    private final OfficeInventoryService officeInventoryService;

    public OfficeInventoryController(OfficeInventoryService officeInventoryService) {
        this.officeInventoryService = officeInventoryService;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAuthority('INVENTARIO_AJUSTAR')")
    public List<OfficeInventoryItemResponse> findItems(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return officeInventoryService.findItems(activeOnly);
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public OfficeInventoryItemResponse createItem(@Valid @RequestBody OfficeInventoryItemRequest request) {
        return officeInventoryService.createItem(request);
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public OfficeInventoryItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody OfficeInventoryItemRequest request) {
        return officeInventoryService.updateItem(id, request);
    }

    @PatchMapping("/items/{id}/status")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public OfficeInventoryItemResponse setItemStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return officeInventoryService.setItemStatus(id, request.active());
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAuthority('INVENTARIO_AJUSTAR')")
    public OfficeInventoryMovementResponse registerMovement(@Valid @RequestBody OfficeInventoryMovementRequest request) {
        return officeInventoryService.registerMovement(request);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('INVENTARIO_AJUSTAR')")
    public List<OfficeInventoryMovementResponse> findMovements(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return officeInventoryService.findMovements(from, to);
    }
}
