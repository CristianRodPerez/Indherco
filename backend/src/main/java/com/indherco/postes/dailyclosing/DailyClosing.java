package com.indherco.postes.dailyclosing;

import com.indherco.postes.shared.enums.DailyClosingStatus;
import com.indherco.postes.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_closings")
public class DailyClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate closingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_user_id", nullable = false)
    private User closedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DailyClosingStatus status = DailyClosingStatus.CERRADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reopened_by_user_id")
    private User reopenedBy;

    private LocalDateTime reopenedAt;

    @Column(length = 500)
    private String reopenReason;

    @Column(nullable = false)
    private Integer totalProduction = 0;

    @Column(nullable = false)
    private Integer totalDispatch = 0;

    @Column(nullable = false)
    private Integer totalConsumption = 0;

    @Column(length = 500)
    private String observation;

    @Column(nullable = false)
    private LocalDateTime closedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public LocalDate getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(LocalDate closingDate) {
        this.closingDate = closingDate;
    }

    public User getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(User closedBy) {
        this.closedBy = closedBy;
    }

    public DailyClosingStatus getStatus() {
        return status;
    }

    public void setStatus(DailyClosingStatus status) {
        this.status = status;
    }

    public User getReopenedBy() {
        return reopenedBy;
    }

    public void setReopenedBy(User reopenedBy) {
        this.reopenedBy = reopenedBy;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(LocalDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public String getReopenReason() {
        return reopenReason;
    }

    public void setReopenReason(String reopenReason) {
        this.reopenReason = reopenReason;
    }

    public Integer getTotalProduction() {
        return totalProduction;
    }

    public void setTotalProduction(Integer totalProduction) {
        this.totalProduction = totalProduction;
    }

    public Integer getTotalDispatch() {
        return totalDispatch;
    }

    public void setTotalDispatch(Integer totalDispatch) {
        this.totalDispatch = totalDispatch;
    }

    public Integer getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(Integer totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
