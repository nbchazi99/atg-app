package com.atg.api.model;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt = LocalDateTime.now();

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
}
