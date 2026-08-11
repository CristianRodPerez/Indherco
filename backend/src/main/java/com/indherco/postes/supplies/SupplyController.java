package com.indherco.postes.supplies;

import com.indherco.postes.supplies.dto.SupplyRequest;
import com.indherco.postes.supplies.dto.SupplyResponse;
import com.indherco.postes.users.dto.StatusRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/supplies")
public class SupplyController {

    private final SupplyService supplyService;

    public SupplyController(SupplyService supplyService) {
        this.supplyService = supplyService;
    }

    @GetMapping
    public List<SupplyResponse> findAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return activeOnly ? supplyService.findActive() : supplyService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public SupplyResponse create(@Valid @RequestBody SupplyRequest request) {
        return supplyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public SupplyResponse update(@PathVariable Long id, @Valid @RequestBody SupplyRequest request) {
        return supplyService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
    public SupplyResponse setStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return supplyService.setStatus(id, request.active());
    }
}
