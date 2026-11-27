package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * One payment attempt through the Dischub gateway. The {@link #orderId} is the
 * unique reference shared with the provider (and shown to the user as the
 * receipt reference); it is the only key the webhook and the client poll on.
 */
public class PaymentTransaction {

    public enum Type { COURSE, VIP_DERIV, VIP_WELTRADE }
    public enum Status { CREATED, PROCESSING, SUCCESS, FAILED, EXPIRED, REFUNDED }

    private Long id;

    private String orderId;
    private Long userId;
    private Type type;
    private Long courseId;
    private String serviceName;
    private BigDecimal amount = BigDecimal.ZERO;
    private String currency = "USD";
    private String sender;
    private String provider = "dischub";

    private Status status = Status.CREATED;
    private String statusReason;
    private String receiptNo;

    private Long purchaseRequestId;
    private Long transactionId;

    // Gateway-reported detail (from the Dischub status response)
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String gatewayTimestamp;
    /** Raw JSON Dischub returned on the last status check, for the admin view. */
    private String gatewayResponse;
    /** Dischub's EcoCash prompt state at creation, e.g. PENDING SUBSCRIBER VALIDATION. */
    private String ecocashStatus;

    private String refundNote;
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
    private LocalDateTime paidAt;
    private LocalDateTime updatedAt;
    private LocalDateTime refundedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public Long getPurchaseRequestId() { return purchaseRequestId; }
    public void setPurchaseRequestId(Long purchaseRequestId) { this.purchaseRequestId = purchaseRequestId; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getGatewayTimestamp() { return gatewayTimestamp; }
    public void setGatewayTimestamp(String gatewayTimestamp) { this.gatewayTimestamp = gatewayTimestamp; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public String getEcocashStatus() { return ecocashStatus; }
    public void setEcocashStatus(String ecocashStatus) { this.ecocashStatus = ecocashStatus; }
    public String getRefundNote() { return refundNote; }
    public void setRefundNote(String refundNote) { this.refundNote = refundNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
