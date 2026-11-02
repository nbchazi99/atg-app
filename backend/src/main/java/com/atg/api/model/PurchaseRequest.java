package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseRequest {

    public enum Type { COURSE, VIP_DERIV, VIP_WELTRADE }
    public enum Status { PENDING, APPROVED, REJECTED }

    private Long id;

    private Long userId;
    private Type type;
    private Long courseId;
    private BigDecimal amount = BigDecimal.ZERO;
    private String proofUrl;

    private Status status = Status.PENDING;

    private Long transactionId;

    private String adminNote;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }
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
