package com.atg.api.web;

import com.atg.api.model.*;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController extends BaseController {

    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final RealtimeHub realtime;

    public CourseController(UserRepository users, CourseRepository courses,
                            LessonRepository lessons, EnrollmentRepository enrollments,
                            RealtimeHub realtime) {
        super(users);
        this.courses = courses;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.realtime = realtime;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        Long userId = me().getId();
        return courses.findAll().stream().map(c -> {
            var enrollment = enrollments.findByUserIdAndCourseId(userId, c.getId());
            List<Lesson> courseLessons = lessons.findByCourseIdOrderByOrderIndexAsc(c.getId());
            long completed = enrollment.map(e -> parseIds(e.getCompletedLessonIds()).stream()
                    .filter(id -> courseLessons.stream().anyMatch(l -> l.getId().equals(id))).count()).orElse(0L);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("course", c);
            m.put("enrolled", enrollment.isPresent());
            m.put("completedLessons", completed);
            m.put("totalLessons", courseLessons.size());
            m.put("progressPct", courseLessons.isEmpty() ? 0 : (int) Math.round(completed * 100.0 / courseLessons.size()));
            return m;
        }).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Course course = courses.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        var enrollment = enrollments.findByUserIdAndCourseId(me().getId(), id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("course", course);
        res.put("lessons", lessons.findByCourseIdOrderByOrderIndexAsc(id));
        res.put("enrolled", enrollment.isPresent());
        res.put("completedLessonIds", enrollment.map(e -> parseIds(e.getCompletedLessonIds())).orElse(List.of()));
        return res;
    }

    @PostMapping("/{id}/complete")
    public Map<String, Object> complete(@PathVariable Long id, @RequestBody Dtos.CompleteReq req) {
        Enrollment e = enrollments.findByUserIdAndCourseId(me().getId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Buy this course first"));
        List<Long> ids = new ArrayList<>(parseIds(e.getCompletedLessonIds()));
        if (!ids.contains(req.lessonId())) ids.add(req.lessonId());
        e.setCompletedLessonIds(serializeIds(ids));
        enrollments.save(e);
        realtime.publish(RealtimeHub.COURSES, Map.of("courseId", id, "kind", "progress"));
        return Map.of("completedLessonIds", ids);
    }

    static List<Long> parseIds(String raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) return ids;
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                try { ids.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        return ids;
    }

    static String serializeIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder(",");
        for (Long id : ids) sb.append(id).append(",");
        return sb.toString();
    }
}
