package com.indherco.postes.stockmovements;

import com.indherco.postes.users.User;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByMovementDateOrderByRegisteredAtDesc(LocalDate movementDate);

    List<StockMovement> findByMovementDateBetweenOrderByRegisteredAtDesc(LocalDate from, LocalDate to);

    List<StockMovement> findByRegisteredByOrderByRegisteredAtDesc(User registeredBy);

    List<StockMovement> findTop10ByOrderByRegisteredAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from StockMovement m where m.id = :id")
    Optional<StockMovement> findByIdForUpdate(@Param("id") Long id);
}
