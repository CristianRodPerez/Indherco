package com.indherco.postes.officeinventory;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeInventoryMovementRepository extends JpaRepository<OfficeInventoryMovement, Long> {

    List<OfficeInventoryMovement> findByMovementDateBetweenOrderByRegisteredAtDesc(LocalDate from, LocalDate to);
}
