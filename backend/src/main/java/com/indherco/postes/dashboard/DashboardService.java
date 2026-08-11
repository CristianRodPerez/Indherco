package com.indherco.postes.dashboard;

import com.indherco.postes.alerts.AlertResponse;
import com.indherco.postes.alerts.AlertService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.dashboard.dto.DashboardResponse;
import com.indherco.postes.products.ProductMapper;
import com.indherco.postes.products.ProductRepository;
import com.indherco.postes.shared.enums.AlertLevel;
import com.indherco.postes.shared.enums.AlertType;
import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.stockmovements.StockMovement;
import com.indherco.postes.stockmovements.StockMovementRepository;
import com.indherco.postes.stockmovements.StockMovementService;
import com.indherco.postes.supplies.SupplyMapper;
import com.indherco.postes.supplies.SupplyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final LocalTime DAILY_RECORD_ALERT_TIME = LocalTime.of(15, 0);

    private final StockMovementRepository movementRepository;
    private final DailyAlertDismissalRepository dismissalRepository;
    private final StockMovementService movementService;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final ProductMapper productMapper;
    private final SupplyMapper supplyMapper;
    private final AlertService alertService;
    private final CurrentUserService currentUserService;

    public DashboardService(
        StockMovementRepository movementRepository,
        DailyAlertDismissalRepository dismissalRepository,
        StockMovementService movementService,
        ProductRepository productRepository,
        SupplyRepository supplyRepository,
        ProductMapper productMapper,
        SupplyMapper supplyMapper,
        AlertService alertService,
        CurrentUserService currentUserService
    ) {
        this.movementRepository = movementRepository;
        this.dismissalRepository = dismissalRepository;
        this.movementService = movementService;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
        this.productMapper = productMapper;
        this.supplyMapper = supplyMapper;
        this.alertService = alertService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse current() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<StockMovement> todayMovements = movementRepository.findByMovementDateOrderByRegisteredAtDesc(today);
        List<StockMovement> monthMovements = movementRepository.findByMovementDateBetweenOrderByRegisteredAtDesc(monthStart, today);
        Set<AlertType> dismissedToday = dismissalRepository.findByAlertDate(today).stream()
            .map(DailyAlertDismissal::getAlertType)
            .collect(Collectors.toSet());

        List<AlertResponse> activeAlerts = Stream.concat(
            pendingDailyRecordAlerts(todayMovements, dismissedToday).stream(),
            alertService.findActive().stream()
        ).toList();

        return new DashboardResponse(
            sum(todayMovements, MovementType.PRODUCCION),
            sum(monthMovements, MovementType.PRODUCCION),
            sum(todayMovements, MovementType.DESPACHO),
            sum(monthMovements, MovementType.DESPACHO),
            sum(todayMovements, MovementType.CONSUMO),
            sum(monthMovements, MovementType.CONSUMO),
            productRepository.findByActiveTrueOrderByNameAsc().stream().map(productMapper::toResponse).toList(),
            supplyRepository.findByActiveTrueOrderByNameAsc().stream().map(supplyMapper::toResponse).toList(),
            activeAlerts,
            movementService.findLatest()
        );
    }

    @Transactional
    public DashboardResponse dismissDailyAlert(AlertType type) {
        if (!isDismissibleDailyAlert(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Esta alerta no se puede ocultar desde el dashboard.");
        }

        LocalDate today = LocalDate.now();
        dismissalRepository.findByAlertDateAndAlertType(today, type)
            .orElseGet(() -> {
                DailyAlertDismissal dismissal = new DailyAlertDismissal();
                dismissal.setAlertDate(today);
                dismissal.setAlertType(type);
                dismissal.setDismissedBy(currentUserService.getCurrentUser());
                dismissal.setDismissedAt(LocalDateTime.now());
                return dismissalRepository.save(dismissal);
            });

        return current();
    }

    private Integer sum(List<StockMovement> movements, MovementType type) {
        return movements.stream()
            .filter(movement -> movement.getMovementType() == type)
            .map(StockMovement::getQuantity)
            .reduce(0, Integer::sum);
    }

    private List<AlertResponse> pendingDailyRecordAlerts(List<StockMovement> todayMovements, Set<AlertType> dismissedToday) {
        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalTime().isBefore(DAILY_RECORD_ALERT_TIME)) {
            return List.of();
        }

        return Stream.of(
                pendingAlert(
                    -1L,
                    AlertType.REGISTRO_PRODUCCION_PENDIENTE,
                    "Todavia no hay produccion registrada hoy. Avisar al encargado que registre la produccion en la aplicacion.",
                    hasMovement(todayMovements, MovementType.PRODUCCION),
                    dismissedToday,
                    now
                ),
                pendingAlert(
                    -2L,
                    AlertType.REGISTRO_DESPACHO_PENDIENTE,
                    "Todavia no hay despachos registrados hoy. Confirmar si hubo despacho y pedir registro por la aplicacion.",
                    hasMovement(todayMovements, MovementType.DESPACHO),
                    dismissedToday,
                    now
                ),
                pendingAlert(
                    -3L,
                    AlertType.REGISTRO_CONSUMO_PENDIENTE,
                    "Todavia no hay consumos registrados hoy. Avisar al responsable de insumos que registre el consumo diario.",
                    hasMovement(todayMovements, MovementType.CONSUMO),
                    dismissedToday,
                    now
                )
            )
            .filter(alert -> alert != null)
            .toList();
    }

    private AlertResponse pendingAlert(Long id, AlertType type, String message, boolean alreadyRegistered, Set<AlertType> dismissedToday, LocalDateTime createdAt) {
        if (alreadyRegistered || dismissedToday.contains(type)) {
            return null;
        }
        return new AlertResponse(id, type, message, AlertLevel.ADVERTENCIA, true, createdAt, null);
    }

    private boolean hasMovement(List<StockMovement> movements, MovementType type) {
        return movements.stream()
            .anyMatch(movement -> movement.getMovementType() == type);
    }

    private boolean isDismissibleDailyAlert(AlertType type) {
        return type == AlertType.REGISTRO_PRODUCCION_PENDIENTE
            || type == AlertType.REGISTRO_DESPACHO_PENDIENTE
            || type == AlertType.REGISTRO_CONSUMO_PENDIENTE;
    }
}
