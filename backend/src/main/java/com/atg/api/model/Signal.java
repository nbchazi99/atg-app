package com.atg.api.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class Signal {

    public enum Side { BUY, SELL }
    public enum Status { LIVE, OPEN, INNER, CLOSED }

    private Long id;

    private String title;
    private String subtitle;
    private String symbol;

    private Side side = Side.BUY;

    private Status status = Status.LIVE;

    private String roomLabel;
    private String entry;
    private String stopLoss;
    private String targets;
    private String ruleName;
    private int participants = 0;
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
    private String result;
    private String closingPrice;
    private String closingUpdate;
    private LocalDateTime closedAt;
    private List<String> updateHistory = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getRoomLabel() { return roomLabel; }
    public void setRoomLabel(String roomLabel) { this.roomLabel = roomLabel; }
    public String getEntry() { return entry; }
    public void setEntry(String entry) { this.entry = entry; }
    public String getStopLoss() { return stopLoss; }
    public void setStopLoss(String stopLoss) { this.stopLoss = stopLoss; }
    public String getTargets() { return targets; }
    public void setTargets(String targets) { this.targets = targets; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public int getParticipants() { return participants; }
    public void setParticipants(int participants) { this.participants = participants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getClosingPrice() { return closingPrice; }
    public void setClosingPrice(String closingPrice) { this.closingPrice = closingPrice; }
    public String getClosingUpdate() { return closingUpdate; }
    public void setClosingUpdate(String closingUpdate) { this.closingUpdate = closingUpdate; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public List<String> getUpdateHistory() { return updateHistory; }
    public void setUpdateHistory(List<String> updateHistory) {
        this.updateHistory = updateHistory == null ? new ArrayList<>() : updateHistory;
    }
}
