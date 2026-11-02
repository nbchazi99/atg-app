package com.atg.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_posts")
public class RoomPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;
    private String badge;
    private String title;
    @Enumerated(EnumType.STRING)
    private Signal.Side side = Signal.Side.BUY;
    private String preview;
    private int participants = 0;
    private boolean open = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Signal.Side getSide() { return side; }
    public void setSide(Signal.Side side) { this.side = side; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public int getParticipants() { return participants; }
    public void setParticipants(int participants) { this.participants = participants; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
