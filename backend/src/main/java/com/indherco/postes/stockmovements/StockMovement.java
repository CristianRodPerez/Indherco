package com.indherco.postes.stockmovements;

import com.indherco.postes.products.Product;
import com.indherco.postes.shared.enums.EntityType;
import com.indherco.postes.shared.enums.MovementStatus;
import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.shared.enums.RegisterOrigin;
import com.indherco.postes.supplies.Supply;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementType movementType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntityType entityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id")
    private Supply supply;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 30)
    private String unitOfMeasure;

    @NotNull
    @Column(nullable = false)
    private Integer previousStock;

    @NotNull
    @Column(nullable = false)
    private Integer newStock;

    @Column(length = 500)
    private String observation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_user_id", nullable = false)
    private User registeredBy;

    @NotNull
    @Column(nullable = false)
    private LocalDate movementDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementStatus status = MovementStatus.ACTIVO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegisterOrigin origin = RegisterOrigin.WEB;

    private Integer rejectedQuantity;

    @Column(length = 50)
    private String shift;

    @Column(length = 160)
    private String clientOrDestination;

    @Column(length = 80)
    private String transport;

    @Column(length = 60)
    private String guideNumber;

    @Column(length = 80)
    private String processArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledBy;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String cancellationReason;

    private Long reversalMovementId;

    @PrePersist
    void onCreate() {
        registeredAt = LocalDateTime.now();
        if (movementDate == null) {
            movementDate = registeredAt.toLocalDate();
        }
    }

    public Long getId() {
        return id;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Supply getSupply() {
        return supply;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public User getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(User registeredBy) {
        this.registeredBy = registeredBy;
    }

    public LocalDate getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(LocalDate movementDate) {
        this.movementDate = movementDate;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public MovementStatus getStatus() {
        return status;
    }

    public void setStatus(MovementStatus status) {
        this.status = status;
    }

    public RegisterOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(RegisterOrigin origin) {
        this.origin = origin;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getClientOrDestination() {
        return clientOrDestination;
    }

    public void setClientOrDestination(String clientOrDestination) {
        this.clientOrDestination = clientOrDestination;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getGuideNumber() {
        return guideNumber;
    }

    public void setGuideNumber(String guideNumber) {
        this.guideNumber = guideNumber;
    }

    public String getProcessArea() {
        return processArea;
    }

    public void setProcessArea(String processArea) {
        this.processArea = processArea;
    }

    public User getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(User cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getReversalMovementId() {
        return reversalMovementId;
    }

    public void setReversalMovementId(Long reversalMovementId) {
        this.reversalMovementId = reversalMovementId;
    }
}
