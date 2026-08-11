package com.indherco.postes.stockmovements;

import com.indherco.postes.alerts.AlertService;
import com.indherco.postes.audit.AuditService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.dailyclosing.DailyClosingService;
import com.indherco.postes.idempotency.IdempotencyService;
import com.indherco.postes.products.Product;
import com.indherco.postes.products.ProductRepository;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.stockmovements.dto.DispatchRequest;
import com.indherco.postes.stockmovements.dto.MovementResponse;
import com.indherco.postes.stockmovements.dto.ProductionRequest;
import com.indherco.postes.supplies.SupplyRepository;
import com.indherco.postes.users.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    StockMovementRepository movementRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    SupplyRepository supplyRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AlertService alertService;

    @Mock
    AuditService auditService;

    @Mock
    DailyClosingService dailyClosingService;

    @Mock
    IdempotencyService idempotencyService;

    @Spy
    StockMovementMapper movementMapper;

    @InjectMocks
    StockMovementService stockMovementService;

    @Test
    void productionIncreasesProductStockAndStoresPreviousAndNewStock() {
        User user = operatorWithProductionPermission();
        Product product = product("Poste 8m", 10);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(idempotencyService.findExistingResultId("StockMovement")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovementResponse response = stockMovementService.registerProduction(new ProductionRequest(null, 1L, 5, 0, null, "turno dia"));

        assertThat(product.getCurrentStock()).isEqualTo(15);
        assertThat(response.previousStock()).isEqualTo(10);
        assertThat(response.newStock()).isEqualTo(15);
        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    void dispatchDoesNotAllowStockToGoNegative() {
        User user = operatorWithDispatchPermission();
        Product product = product("Poste 8m", 3);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(idempotencyService.findExistingResultId("StockMovement")).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        DispatchRequest request = new DispatchRequest(null, 1L, 4, null, null, null, null);

        assertThatThrownBy(() -> stockMovementService.registerDispatch(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("No hay stock suficiente para despachar esta cantidad.");

        assertThat(product.getCurrentStock()).isEqualTo(3);
        verify(movementRepository, never()).save(any());
    }

    private User operatorWithProductionPermission() {
        User user = new User();
        user.setName("Operador");
        user.setUsername("operador");
        user.setPasswordHash("hash");
        user.setCanRegisterProduction(true);
        return user;
    }

    private User operatorWithDispatchPermission() {
        User user = new User();
        user.setName("Despacho");
        user.setUsername("despacho");
        user.setPasswordHash("hash");
        user.setCanRegisterDispatch(true);
        return user;
    }

    private Product product(String name, Integer stock) {
        Product product = new Product();
        product.setName(name);
        product.setType("Poste");
        product.setUnitOfMeasure("unidad");
        product.setCurrentStock(stock);
        return product;
    }
}
