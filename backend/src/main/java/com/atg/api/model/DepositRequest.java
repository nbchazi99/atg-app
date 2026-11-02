package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    private Long id;

    private Long userId;
    private String broker;
    private String clientId;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal feePct = BigDecimal.ZERO;
    private BigDecimal fee = BigDecimal.ZERO;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private String wallet;
    private String mobile;

    private Status status = Status.PENDING;

    private Long transactionId;

    private String adminNote;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeePct() { return feePct; }
    public void setFeePct(BigDecimal feePct) { this.feePct = feePct; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public String getWallet() { return wallet; }
    public void setWallet(String wallet) { this.wallet = wallet; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
