package com.atg.api.web;

import com.atg.api.model.AcademyComment;
import com.atg.api.model.Course;
import com.atg.api.model.Lesson;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.AcademyCommentRepository;
import com.atg.api.repo.CourseRepository;
import com.atg.api.repo.LessonRepository;
import com.atg.api.repo.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class AcademyCommentController extends BaseController {

    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final AcademyCommentRepository comments;
    private final RealtimeHub realtime;

    public AcademyCommentController(UserRepository users, CourseRepository courses,
                                    LessonRepository lessons, AcademyCommentRepository comments,
                                    RealtimeHub realtime) {
        super(users);
        this.courses = courses;
        this.lessons = lessons;
        this.comments = comments;
        this.realtime = realtime;
    }

    @GetMapping("/{courseId}/comments")
    public List<AcademyComment> courseComments(@PathVariable Long courseId) {
        requireCourse(courseId);
        return comments.findByCourseIdAndLessonId(courseId, null);
    }

    @PostMapping("/{courseId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyComment addCourseComment(@PathVariable Long courseId, @Valid @RequestBody CommentReq req) {
        requireCourse(courseId);
        return save(courseId, null, req);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}/comments")
    public List<AcademyComment> lessonComments(@PathVariable Long courseId, @PathVariable Long lessonId) {
        requireLesson(courseId, lessonId);
        return comments.findByCourseIdAndLessonId(courseId, lessonId);
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademyComment addLessonComment(@PathVariable Long courseId, @PathVariable Long lessonId,
                                           @Valid @RequestBody CommentReq req) {
        requireLesson(courseId, lessonId);
        return save(courseId, lessonId, req);
    }

    private AcademyComment save(Long courseId, Long lessonId, CommentReq req) {
        User user = me();
        AcademyComment comment = new AcademyComment();
        comment.setCourseId(courseId);
        comment.setLessonId(lessonId);
        comment.setAuthorId(user.getId());
        comment.setAuthorName(user.getName());
        comment.setText(req.text().trim());
        comment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        AcademyComment saved = comments.save(comment);
        realtime.publish(RealtimeHub.COURSES, java.util.Map.of(
                "courseId", courseId,
                "kind", "comment"));
        return saved;
    }

    private Course requireCourse(Long courseId) {
        return courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private Lesson requireLesson(Long courseId, Long lessonId) {
        requireCourse(courseId);
        return lessons.findById(lessonId)
                .filter(lesson -> courseId.equals(lesson.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found in this course"));
    }

    public record CommentReq(
            @NotBlank @Size(max = 2000, message = "Comment must be 2000 characters or fewer") String text) {}
}
