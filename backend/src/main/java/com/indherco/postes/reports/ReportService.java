package com.indherco.postes.reports;

import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.stockmovements.StockMovement;
import com.indherco.postes.stockmovements.StockMovementMapper;
import com.indherco.postes.stockmovements.StockMovementRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final StockMovementRepository movementRepository;
    private final StockMovementMapper movementMapper;

    public ReportService(StockMovementRepository movementRepository, StockMovementMapper movementMapper) {
        this.movementRepository = movementRepository;
        this.movementMapper = movementMapper;
    }

    public ReportSummaryResponse daily(LocalDate date) {
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        return summary(reportDate, reportDate);
    }

    public ReportSummaryResponse monthly(YearMonth month) {
        YearMonth reportMonth = month == null ? YearMonth.now() : month;
        return summary(reportMonth.atDay(1), reportMonth.atEndOfMonth());
    }

    public String exportCsv(LocalDate from, LocalDate to) {
        ReportSummaryResponse report = summary(from, to);
        StringBuilder builder = new StringBuilder();
        builder.append("fecha,tipo,item,cantidad,unidad,stock_anterior,stock_nuevo,usuario,observacion\n");
        report.movements().forEach(movement -> builder
            .append(movement.movementDate()).append(',')
            .append(movement.movementType()).append(',')
            .append(escape(movement.itemName())).append(',')
            .append(movement.quantity()).append(',')
            .append(escape(movement.unitOfMeasure())).append(',')
            .append(movement.previousStock()).append(',')
            .append(movement.newStock()).append(',')
            .append(escape(movement.registeredBy())).append(',')
            .append(escape(movement.observation()))
            .append('\n')
        );
        return builder.toString();
    }

    private ReportSummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        List<StockMovement> movements = movementRepository.findByMovementDateBetweenOrderByRegisteredAtDesc(start, end);
        return new ReportSummaryResponse(
            start,
            end,
            sum(movements, MovementType.PRODUCCION),
            sum(movements, MovementType.DESPACHO),
            sum(movements, MovementType.CONSUMO),
            movements.stream().map(movementMapper::toResponse).toList()
        );
    }

    private Integer sum(List<StockMovement> movements, MovementType type) {
        return movements.stream()
            .filter(movement -> movement.getMovementType() == type)
            .map(StockMovement::getQuantity)
            .reduce(0, Integer::sum);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
