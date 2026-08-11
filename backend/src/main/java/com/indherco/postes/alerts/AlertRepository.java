package com.indherco.postes.alerts;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByActiveTrueOrderByCreatedAtDesc();
}
