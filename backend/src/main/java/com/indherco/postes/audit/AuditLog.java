package com.indherco.postes.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 60)
    private String username;

    @Column(nullable = false, length = 80)
    private String module;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 80)
    private String entity;

    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 80)
    private String ip;

    @Column(length = 300)
    private String userAgent;

    @Column(length = 80)
    private String correlationId;

    @Column(columnDefinition = "TEXT")
    private String previousDetail;

    @Column(columnDefinition = "TEXT")
    private String newDetail;

    @Column(length = 500)
    private String reason;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getPreviousDetail() { return previousDetail; }
    public void setPreviousDetail(String previousDetail) { this.previousDetail = previousDetail; }
    public String getNewDetail() { return newDetail; }
    public void setNewDetail(String newDetail) { this.newDetail = newDetail; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
