package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketPair {

    public enum AssetClass { FOREX, METALS, CRYPTO }

    private Long id;

    private String symbol;
    private String name;

    private AssetClass assetClass = AssetClass.FOREX;

    private BigDecimal price = BigDecimal.ZERO;
    private BigDecimal changePct = BigDecimal.ZERO;
    private BigDecimal high = BigDecimal.ZERO;
    private BigDecimal low = BigDecimal.ZERO;
    private BigDecimal prevClose = BigDecimal.ZERO;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetClass getAssetClass() { return assetClass; }
    public void setAssetClass(AssetClass assetClass) { this.assetClass = assetClass; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getChangePct() { return changePct; }
    public void setChangePct(BigDecimal changePct) { this.changePct = changePct; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getPrevClose() { return prevClose; }
    public void setPrevClose(BigDecimal prevClose) { this.prevClose = prevClose; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
