package com.atg.api.model;

import java.time.LocalDateTime;

public class Enrollment {

    private Long id;

    private Long userId;
    private Long courseId;
    /** Comma separated lesson ids, e.g. ",3,4," */
    private String completedLessonIds = "";
    private LocalDateTime enrolledAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCompletedLessonIds() { return completedLessonIds; }
    public void setCompletedLessonIds(String completedLessonIds) { this.completedLessonIds = completedLessonIds; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
}
