package com.indherco.postes.dashboard;

import com.indherco.postes.shared.enums.AlertType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyAlertDismissalRepository extends JpaRepository<DailyAlertDismissal, Long> {

    List<DailyAlertDismissal> findByAlertDate(LocalDate alertDate);

    Optional<DailyAlertDismissal> findByAlertDateAndAlertType(LocalDate alertDate, AlertType alertType);
}
