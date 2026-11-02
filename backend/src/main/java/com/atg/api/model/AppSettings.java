package com.atg.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    private Long id = 1L;

    private BigDecimal adminFeePct = BigDecimal.valueOf(10);
    private BigDecimal withdrawFeePct = BigDecimal.valueOf(7);
    private BigDecimal minDeposit = BigDecimal.valueOf(12.50);
    private BigDecimal maxDeposit = BigDecimal.valueOf(454.54);
    private BigDecimal minWithdraw = BigDecimal.valueOf(12.50);
    private String fact = "Black Monday";
    private String promotionTitle = "MWR Strategy Course Promotion";
    private String promotionBody = "MWR STRATEGY COURSE — FLASH PROMO. Enrol before the window closes.";
    private int pointsPerReferral = 5;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAdminFeePct() { return adminFeePct; }
    public void setAdminFeePct(BigDecimal adminFeePct) { this.adminFeePct = adminFeePct; }
    public BigDecimal getWithdrawFeePct() { return withdrawFeePct; }
    public void setWithdrawFeePct(BigDecimal withdrawFeePct) { this.withdrawFeePct = withdrawFeePct; }
    public BigDecimal getMinDeposit() { return minDeposit; }
    public void setMinDeposit(BigDecimal minDeposit) { this.minDeposit = minDeposit; }
    public BigDecimal getMaxDeposit() { return maxDeposit; }
    public void setMaxDeposit(BigDecimal maxDeposit) { this.maxDeposit = maxDeposit; }
    public BigDecimal getMinWithdraw() { return minWithdraw; }
    public void setMinWithdraw(BigDecimal minWithdraw) { this.minWithdraw = minWithdraw; }
    public String getFact() { return fact; }
    public void setFact(String fact) { this.fact = fact; }
    public String getPromotionTitle() { return promotionTitle; }
    public void setPromotionTitle(String promotionTitle) { this.promotionTitle = promotionTitle; }
    public String getPromotionBody() { return promotionBody; }
    public void setPromotionBody(String promotionBody) { this.promotionBody = promotionBody; }
    public int getPointsPerReferral() { return pointsPerReferral; }
    public void setPointsPerReferral(int pointsPerReferral) { this.pointsPerReferral = pointsPerReferral; }
}
