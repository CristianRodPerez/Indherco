package com.indherco.postes.alerts;

import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.products.Product;
import com.indherco.postes.shared.enums.AlertLevel;
import com.indherco.postes.shared.enums.AlertType;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.supplies.Supply;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final CurrentUserService currentUserService;

    public AlertService(AlertRepository alertRepository, CurrentUserService currentUserService) {
        this.alertRepository = alertRepository;
        this.currentUserService = currentUserService;
    }

    public List<AlertResponse> findActive() {
        return alertRepository.findByActiveTrueOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(AlertType type, AlertLevel level, String message) {
        Alert alert = new Alert();
        alert.setType(type);
        alert.setLevel(level);
        alert.setMessage(message);
        alertRepository.save(alert);
    }

    @Transactional
    public void evaluateProductStock(Product product) {
        Integer minimum = product.getMinimumStock();
        if (minimum != null && product.getCurrentStock() <= minimum) {
            create(AlertType.STOCK_BAJO_PRODUCTO, AlertLevel.ADVERTENCIA, "Stock bajo de producto: " + product.getName());
        }
    }

    @Transactional
    public void evaluateSupplyStock(Supply supply) {
        Integer minimum = supply.getMinimumStock();
        if (minimum != null && supply.getCurrentStock() <= minimum) {
            create(AlertType.STOCK_BAJO_INSUMO, AlertLevel.ADVERTENCIA, "Stock bajo de insumo: " + supply.getName());
        }
    }

    @Transactional
    public AlertResponse resolve(Long id) {
        Alert alert = alertRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alerta no encontrada."));
        alert.setActive(false);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(currentUserService.getCurrentUser());
        return toResponse(alert);
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getType(),
            alert.getMessage(),
            alert.getLevel(),
            alert.isActive(),
            alert.getCreatedAt(),
            alert.getResolvedAt()
        );
    }
}
