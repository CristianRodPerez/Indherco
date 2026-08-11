package com.indherco.postes.dashboard.dto;

import com.indherco.postes.alerts.AlertResponse;
import com.indherco.postes.products.dto.ProductResponse;
import com.indherco.postes.stockmovements.dto.MovementResponse;
import com.indherco.postes.supplies.dto.SupplyResponse;
import java.util.List;

public record DashboardResponse(
    Integer productionToday,
    Integer productionMonth,
    Integer dispatchToday,
    Integer dispatchMonth,
    Integer consumptionToday,
    Integer consumptionMonth,
    List<ProductResponse> productsStock,
    List<SupplyResponse> suppliesStock,
    List<AlertResponse> activeAlerts,
    List<MovementResponse> latestMovements
) {
}
