package com.atg.api.model;

import java.math.BigDecimal;
import java.util.List;

public class AppSettings {

    private Long id = 1L;

    private BigDecimal adminFeePct = BigDecimal.valueOf(10);
    private BigDecimal withdrawFeePct = BigDecimal.valueOf(10);
    private BigDecimal minDeposit = BigDecimal.valueOf(5);
    private BigDecimal maxDeposit = BigDecimal.valueOf(454);
    private BigDecimal minWithdraw = BigDecimal.valueOf(5);
    private BigDecimal maxWithdraw = BigDecimal.valueOf(454);
    private boolean depositMinimumMigrated;
    private boolean financialLimitsMigrated;
    private boolean withdrawFeeMigrated;
    private String fact = "";
    private String promotionTitle = "";
    private String promotionBody = "";
    private int pointsPerReferral = 5;
    private BigDecimal derivVipPrice = BigDecimal.valueOf(30);
    private BigDecimal weltradeVipPrice = BigDecimal.valueOf(30);
    private int vipExpiryDays = 30;
    private List<String> depositMethods = List.of("EcoCash");
    private List<String> withdrawalMethods = List.of("EcoCash");

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
    public BigDecimal getMaxWithdraw() { return maxWithdraw; }
    public void setMaxWithdraw(BigDecimal maxWithdraw) { this.maxWithdraw = maxWithdraw; }
    public boolean isDepositMinimumMigrated() { return depositMinimumMigrated; }
    public void setDepositMinimumMigrated(boolean depositMinimumMigrated) {
        this.depositMinimumMigrated = depositMinimumMigrated;
    }
    public boolean isFinancialLimitsMigrated() { return financialLimitsMigrated; }
    public void setFinancialLimitsMigrated(boolean financialLimitsMigrated) {
        this.financialLimitsMigrated = financialLimitsMigrated;
    }
    public boolean isWithdrawFeeMigrated() { return withdrawFeeMigrated; }
    public void setWithdrawFeeMigrated(boolean withdrawFeeMigrated) {
        this.withdrawFeeMigrated = withdrawFeeMigrated;
    }
    public String getFact() { return fact; }
    public void setFact(String fact) { this.fact = fact; }
    public String getPromotionTitle() { return promotionTitle; }
    public void setPromotionTitle(String promotionTitle) { this.promotionTitle = promotionTitle; }
    public String getPromotionBody() { return promotionBody; }
    public void setPromotionBody(String promotionBody) { this.promotionBody = promotionBody; }
    public int getPointsPerReferral() { return pointsPerReferral; }
    public void setPointsPerReferral(int pointsPerReferral) { this.pointsPerReferral = pointsPerReferral; }
    public BigDecimal getDerivVipPrice() { return derivVipPrice; }
    public void setDerivVipPrice(BigDecimal derivVipPrice) { this.derivVipPrice = derivVipPrice; }
    public BigDecimal getWeltradeVipPrice() { return weltradeVipPrice; }
    public void setWeltradeVipPrice(BigDecimal weltradeVipPrice) { this.weltradeVipPrice = weltradeVipPrice; }
    public int getVipExpiryDays() { return vipExpiryDays; }
    public void setVipExpiryDays(int vipExpiryDays) { this.vipExpiryDays = vipExpiryDays; }
    public List<String> getDepositMethods() { return depositMethods; }
    public void setDepositMethods(List<String> depositMethods) { this.depositMethods = depositMethods; }
    public List<String> getWithdrawalMethods() { return withdrawalMethods; }
    public void setWithdrawalMethods(List<String> withdrawalMethods) { this.withdrawalMethods = withdrawalMethods; }
}
