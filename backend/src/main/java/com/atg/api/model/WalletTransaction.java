package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {

    public enum Type { DEPOSIT, WITHDRAWAL, PURCHASE, REFERRAL }
    public enum Status { PENDING, COMPLETED, REJECTED }

    private Long id;

    private Long userId;
    private Type type;
    private String description;
    private BigDecimal amount = BigDecimal.ZERO;
    private Status status = Status.PENDING;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
