package com.atg.api.model;

import java.time.LocalDateTime;

public class Room {

    private Long id;

    private String name;
    private String description;
    private String preview;
    private int unreadCount = 0;
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
