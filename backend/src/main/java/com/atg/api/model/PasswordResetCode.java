package com.atg.api.model;

import java.time.LocalDateTime;

public class PasswordResetCode {

    private Long id;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime requestedAt;
    private int failedAttempts;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
}
