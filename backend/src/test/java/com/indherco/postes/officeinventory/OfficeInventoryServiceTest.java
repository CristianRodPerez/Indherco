package com.indherco.postes.officeinventory;

import com.indherco.postes.audit.AuditService;
import com.indherco.postes.auth.CurrentUserService;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementRequest;
import com.indherco.postes.officeinventory.dto.OfficeInventoryMovementResponse;
import com.indherco.postes.shared.enums.OfficeInventoryMovementType;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.users.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeInventoryServiceTest {

    @Mock
    OfficeInventoryItemRepository itemRepository;

    @Mock
    OfficeInventoryMovementRepository movementRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AuditService auditService;

    @InjectMocks
    OfficeInventoryService officeInventoryService;

    @Test
    void entryMovementIncreasesOfficeInventoryStock() {
        OfficeInventoryItem item = item("Casco", 2);

        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(currentUserService.getCurrentUser()).thenReturn(user());
        when(movementRepository.save(any(OfficeInventoryMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfficeInventoryMovementResponse response = officeInventoryService.registerMovement(
            new OfficeInventoryMovementRequest(null, 1L, OfficeInventoryMovementType.ENTRADA, 3, "compra")
        );

        assertThat(item.getCurrentStock()).isEqualTo(5);
        assertThat(response.previousStock()).isEqualTo(2);
        assertThat(response.newStock()).isEqualTo(5);
    }

    @Test
    void consumptionMovementDoesNotAllowNegativeStock() {
        OfficeInventoryItem item = item("Guantes", 1);

        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> officeInventoryService.registerMovement(
            new OfficeInventoryMovementRequest(null, 1L, OfficeInventoryMovementType.CONSUMO, 2, "entrega")
        ))
            .isInstanceOf(ApiException.class)
            .hasMessage("No hay stock suficiente para consumir esta cantidad.");

        assertThat(item.getCurrentStock()).isEqualTo(1);
        verify(movementRepository, never()).save(any());
    }

    private OfficeInventoryItem item(String name, Integer stock) {
        OfficeInventoryItem item = new OfficeInventoryItem();
        item.setName(name);
        item.setCategory("Seguridad");
        item.setUnitOfMeasure("unidad");
        item.setCurrentStock(stock);
        return item;
    }

    private User user() {
        User user = new User();
        user.setName("Oficina");
        user.setUsername("oficina");
        user.setPasswordHash("hash");
        return user;
    }
}
