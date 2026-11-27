package com.atg.api.web;

import com.atg.api.config.JwtService;
import com.atg.api.model.Course;
import com.atg.api.model.DepositRequest;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.PaymentService;
import com.atg.api.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerWorkflowTest {

    private final UserRepository users = mock(UserRepository.class);
    private final DepositRequestRepository deposits = mock(DepositRequestRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final AdminController controller = new AdminController(
            users,
            mock(WalletService.class),
            mock(WalletTransactionRepository.class),
            deposits,
            mock(WithdrawalRequestRepository.class),
            mock(PurchaseRequestRepository.class),
            mock(SignalRepository.class),
            mock(RoomRepository.class),
            mock(RoomMessageRepository.class),
            courses,
            mock(LessonRepository.class),
            mock(RealtimeHub.class),
            mock(JwtService.class),
            mock(PasswordEncoder.class),
            mock(PaymentService.class));

    private final User admin = new User();

    @BeforeEach
    void setUp() {
        admin.setId(99L);
        admin.setName("ATG Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(User.Role.ADMIN);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void depositRequestsIncludeTheOwningUsersReviewDetails() {
        User customer = new User();
        customer.setId(12L);
        customer.setName("Trader Twelve");
        customer.setPhone("+263770000012");
        DepositRequest deposit = new DepositRequest();
        deposit.setId(5L);
        deposit.setUserId(12L);
        deposit.setClientId("WT-1234");
        when(users.findAll()).thenReturn(List.of(customer));
        when(deposits.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(deposit));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) controller.requests().get("deposits");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("username", "Trader Twelve");
            assertThat(row).containsEntry("weltradeId", "WT-1234");
            assertThat(row).containsEntry("phone", "+263770000012");
        });
    }

    @Test
    void adminsCanPublishAPendingCourseOnlyOnce() {
        Course course = new Course();
        course.setId(5L);
        course.setReviewStatus(Course.ReviewStatus.PENDING);
        when(courses.findById(5L)).thenReturn(Optional.of(course));
        when(courses.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course published = controller.reviewCourse(
                5L, new Dtos.CourseReviewReq(true, "Materials verified"));

        assertThat(published.getReviewStatus()).isEqualTo(Course.ReviewStatus.PUBLISHED);
        assertThat(published.getReviewedBy()).isEqualTo(99L);
        assertThat(published.getReviewNote()).isEqualTo("Materials verified");
        assertThat(published.getReviewedAt()).isNotNull();
        assertThatThrownBy(() -> controller.reviewCourse(
                5L, new Dtos.CourseReviewReq(false, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been reviewed");
    }
}
