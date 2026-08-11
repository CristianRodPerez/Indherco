package com.indherco.postes.dashboard;

import com.indherco.postes.dashboard.dto.DashboardResponse;
import com.indherco.postes.dashboard.dto.DismissDailyAlertRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN_OFICINA', 'OFICINA')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/today")
    public DashboardResponse today() {
        return dashboardService.current();
    }

    @GetMapping("/month")
    public DashboardResponse month() {
        return dashboardService.current();
    }

    @PostMapping("/daily-alerts/dismiss")
    public DashboardResponse dismissDailyAlert(@Valid @RequestBody DismissDailyAlertRequest request) {
        return dashboardService.dismissDailyAlert(request.type());
    }
}
