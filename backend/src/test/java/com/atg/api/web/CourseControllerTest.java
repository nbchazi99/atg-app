package com.atg.api.web;

import com.atg.api.model.Course;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.CourseRepository;
import com.atg.api.repo.EnrollmentRepository;
import com.atg.api.repo.LessonRepository;
import com.atg.api.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final LessonRepository lessons = mock(LessonRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final User member = new User();
    private CourseController controller;

    @BeforeEach
    void setUp() {
        member.setId(7L);
        member.setName("Trader");
        member.setEmail("trader@example.com");
        when(users.findByEmailIgnoreCase(member.getEmail())).thenReturn(Optional.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member.getEmail(), null, List.of()));
        when(enrollments.findByUserIdAndCourseId(7L, 1L)).thenReturn(Optional.empty());
        when(lessons.findByCourseIdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        controller = new CourseController(
                users, courses, lessons, enrollments, mock(RealtimeHub.class));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void academyListsPublishedCoursesAndHidesPendingSubmissions() {
        Course published = new Course();
        published.setId(1L);
        Course pending = new Course();
        pending.setId(2L);
        pending.setReviewStatus(Course.ReviewStatus.PENDING);
        when(courses.findAll()).thenReturn(List.of(published, pending));

        assertThat(controller.list())
                .extracting(row -> ((Dtos.CourseView) row.get("course")).id())
                .containsExactly(1L);
    }

    @Test
    void learnersCannotOpenOrEnrollInUnpublishedCourses() {
        Course pending = new Course();
        pending.setId(2L);
        pending.setReviewStatus(Course.ReviewStatus.PENDING);
        when(courses.findById(2L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> controller.detail(2L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> controller.enroll(2L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
