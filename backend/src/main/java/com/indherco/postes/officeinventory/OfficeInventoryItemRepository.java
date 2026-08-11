package com.indherco.postes.officeinventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfficeInventoryItemRepository extends JpaRepository<OfficeInventoryItem, Long> {

    List<OfficeInventoryItem> findByActiveTrueOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from OfficeInventoryItem i where i.id = :id")
    Optional<OfficeInventoryItem> findByIdForUpdate(@Param("id") Long id);
}
