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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademyCommentControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final LessonRepository lessons = mock(LessonRepository.class);
    private final AcademyCommentRepository comments = mock(AcademyCommentRepository.class);
    private final RealtimeHub realtime = mock(RealtimeHub.class);
    private final User member = new User();
    private AcademyCommentController controller;

    @BeforeEach
    void setUp() {
        member.setId(7L);
        member.setName("Trader");
        member.setEmail("trader@example.com");
        when(users.findByEmailIgnoreCase(member.getEmail())).thenReturn(Optional.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member.getEmail(), null, List.of()));
        controller = new AcademyCommentController(users, courses, lessons, comments, realtime);

        Course course = new Course();
        course.setId(12L);
        when(courses.findById(12L)).thenReturn(Optional.of(course));
        when(comments.save(any(AcademyComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void memberCanCommentOnCourseAndLesson() {
        Lesson lesson = new Lesson();
        lesson.setId(31L);
        lesson.setCourseId(12L);
        when(lessons.findById(31L)).thenReturn(Optional.of(lesson));

        AcademyComment courseComment = controller.addCourseComment(
                12L, new AcademyCommentController.CommentReq("  Helpful course!  "));
        AcademyComment lessonComment = controller.addLessonComment(
                12L, 31L, new AcademyCommentController.CommentReq("  https://example.com  "));

        assertThat(courseComment.getCourseId()).isEqualTo(12L);
        assertThat(courseComment.getLessonId()).isNull();
        assertThat(courseComment.getAuthorId()).isEqualTo(member.getId());
        assertThat(courseComment.getText()).isEqualTo("Helpful course!");
        assertThat(lessonComment.getLessonId()).isEqualTo(31L);
        assertThat(lessonComment.getText()).isEqualTo("https://example.com");
        org.mockito.Mockito.verify(realtime, org.mockito.Mockito.times(2)).publish(
                RealtimeHub.COURSES, java.util.Map.of("courseId", 12L, "kind", "comment"));
    }

    @Test
    void rejectsCommentsForLessonsFromAnotherCourse() {
        Lesson lesson = new Lesson();
        lesson.setId(31L);
        lesson.setCourseId(99L);
        when(lessons.findById(31L)).thenReturn(Optional.of(lesson));

        assertThatThrownBy(() -> controller.addLessonComment(
                12L, 31L, new AcademyCommentController.CommentReq("Not this course")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Lesson not found in this course");
    }
}
