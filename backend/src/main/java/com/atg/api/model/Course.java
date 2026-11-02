package com.atg.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Course {

    private Long id;

    private String title;
    private String subtitle;
    private String description;
    private BigDecimal price = BigDecimal.ZERO;
    private String level = "Beginner";
    private String imageUrl;
    private int lessonsCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getLessonsCount() { return lessonsCount; }
    public void setLessonsCount(int lessonsCount) { this.lessonsCount = lessonsCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
