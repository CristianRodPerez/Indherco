package com.indherco.postes.supplies;

import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.supplies.dto.SupplyRequest;
import com.indherco.postes.supplies.dto.SupplyResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplyService {

    private final SupplyRepository supplyRepository;
    private final SupplyMapper supplyMapper;

    public SupplyService(SupplyRepository supplyRepository, SupplyMapper supplyMapper) {
        this.supplyRepository = supplyRepository;
        this.supplyMapper = supplyMapper;
    }

    public List<SupplyResponse> findAll() {
        return supplyRepository.findAll().stream().map(supplyMapper::toResponse).toList();
    }

    public List<SupplyResponse> findActive() {
        return supplyRepository.findByActiveTrueOrderByNameAsc().stream().map(supplyMapper::toResponse).toList();
    }

    @Transactional
    public SupplyResponse create(SupplyRequest request) {
        Supply supply = new Supply();
        apply(supply, request, true);
        return supplyMapper.toResponse(supplyRepository.save(supply));
    }

    @Transactional
    public SupplyResponse update(Long id, SupplyRequest request) {
        Supply supply = supplyRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insumo no encontrado."));
        apply(supply, request, false);
        return supplyMapper.toResponse(supply);
    }

    @Transactional
    public SupplyResponse setStatus(Long id, boolean active) {
        Supply supply = supplyRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insumo no encontrado."));
        supply.setActive(active);
        return supplyMapper.toResponse(supply);
    }

    private void apply(Supply supply, SupplyRequest request, boolean allowInitialStock) {
        supply.setName(request.name());
        supply.setCategory(request.category());
        supply.setUnitOfMeasure(request.unitOfMeasure());
        supply.setMinimumStock(request.minimumStock());
        if (allowInitialStock) {
            supply.setCurrentStock(request.currentStock() == null ? 0 : request.currentStock());
        }
    }
}
