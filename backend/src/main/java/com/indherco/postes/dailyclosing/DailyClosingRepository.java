package com.indherco.postes.dailyclosing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyClosingRepository extends JpaRepository<DailyClosing, Long> {

    Optional<DailyClosing> findByClosingDate(LocalDate closingDate);

    boolean existsByClosingDate(LocalDate closingDate);

    List<DailyClosing> findTop60ByOrderByClosingDateDesc();
}
