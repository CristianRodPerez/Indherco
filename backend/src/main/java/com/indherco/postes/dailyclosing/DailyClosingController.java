package com.indherco.postes.dailyclosing;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-closing")
public class DailyClosingController {

    private final DailyClosingService dailyClosingService;

    public DailyClosingController(DailyClosingService dailyClosingService) {
        this.dailyClosingService = dailyClosingService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CIERRE_DIARIO')")
    public DailyClosingResponse closeDay(@RequestBody DailyClosingRequest request) {
        return dailyClosingService.closeDay(request);
    }

    @GetMapping("/{date}")
    @PreAuthorize("hasAuthority('CIERRE_DIARIO')")
    public DailyClosingResponse findByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyClosingService.findByDate(date);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CIERRE_DIARIO')")
    public List<DailyClosingResponse> findLatest() {
        return dailyClosingService.findLatest();
    }

    @PostMapping("/{date}/reopen")
    @PreAuthorize("hasAuthority('CIERRE_REABRIR')")
    public DailyClosingResponse reopenDay(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestBody ReopenDailyClosingRequest request
    ) {
        return dailyClosingService.reopenDay(date, request);
    }
}
