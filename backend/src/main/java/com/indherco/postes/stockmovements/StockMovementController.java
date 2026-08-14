package com.indherco.postes.stockmovements;

import com.indherco.postes.stockmovements.dto.CancelMovementRequest;
import com.indherco.postes.stockmovements.dto.ConsumptionRequest;
import com.indherco.postes.stockmovements.dto.DispatchRequest;
import com.indherco.postes.stockmovements.dto.MovementResponse;
import com.indherco.postes.stockmovements.dto.ProductionRequest;
import com.indherco.postes.stockmovements.dto.SupplyReceiptRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class StockMovementController {

    private final StockMovementService movementService;

    public StockMovementController(StockMovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/production")
    @PreAuthorize("hasAuthority('PRODUCCION_CREAR')")
    public MovementResponse production(@Valid @RequestBody ProductionRequest request) {
        return movementService.registerProduction(request);
    }

    @PostMapping("/dispatch")
    @PreAuthorize("hasAuthority('DESPACHO_CREAR')")
    public MovementResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        return movementService.registerDispatch(request);
    }

    @PostMapping("/consumption")
    @PreAuthorize("hasAuthority('CONSUMO_CREAR')")
    public MovementResponse consumption(@Valid @RequestBody ConsumptionRequest request) {
        return movementService.registerConsumption(request);
    }

    @PostMapping("/supply-receipt")
    @PreAuthorize("hasAnyRole('ADMIN_OFICINA', 'OFICINA') or hasAuthority('CONSUMO_CREAR')")
    public MovementResponse supplyReceipt(@Valid @RequestBody SupplyReceiptRequest request) {
        return movementService.registerSupplyReceipt(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCCION_VER', 'DESPACHO_VER', 'CONSUMO_VER', 'AUDITORIA_VER')")
    public List<MovementResponse> findAll(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return movementService.findAll(from, to);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('PRODUCCION_VER', 'DESPACHO_VER', 'CONSUMO_VER')")
    public List<MovementResponse> findMine() {
        return movementService.findMine();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('DESPACHO_ANULAR')")
    public MovementResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelMovementRequest request) {
        return movementService.cancelMovement(id, request);
    }
}
