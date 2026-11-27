package com.atg.api.store;

import com.atg.api.model.Course;
import com.atg.api.model.Signal;
import com.atg.api.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperWorkflowFieldsTest {

    @Test
    void storesSignalCloseDetailsAndUpdateHistoryInFirestoreDocumentShape() {
        Signal signal = new Signal();
        signal.setStatus(Signal.Status.CLOSED);
        signal.setResult("TP_HIT");
        signal.setClosingPrice("1.0920");
        signal.setClosingUpdate("Target reached");
        signal.setClosedAt(LocalDateTime.parse("2026-11-27T12:30:00"));
        signal.setUpdateHistory(List.of("created", "closed"));

        Map<String, Object> document = EntityMapper.toDoc(signal);

        assertThat(document)
                .containsEntry("status", "CLOSED")
                .containsEntry("result", "TP_HIT")
                .containsEntry("closingPrice", "1.0920")
                .containsEntry("closingUpdate", "Target reached")
                .containsEntry("closedAt", "2026-11-27T12:30")
                .containsEntry("updateHistory", List.of("created", "closed"));
    }

    @Test
    void storesCourseApprovalStateForPersistedReview() {
        Course course = new Course();
        course.setReviewStatus(Course.ReviewStatus.PENDING);
        course.setReviewedBy(99L);
        course.setReviewedAt(LocalDateTime.parse("2026-11-27T12:30:00"));
        course.setReviewNote("Materials verified");

        Map<String, Object> document = EntityMapper.toDoc(course);

        assertThat(document)
                .containsEntry("reviewStatus", "PENDING")
                .containsEntry("reviewedBy", 99L)
                .containsEntry("reviewedAt", "2026-11-27T12:30")
                .containsEntry("reviewNote", "Materials verified");
    }

    @Test
    void storesNewProfileFieldsInFirestoreDocumentShape() {
        User user = new User();
        user.setUsername("trader");
        user.setCountry("Kenya");

        Map<String, Object> document = EntityMapper.toDoc(user);

        assertThat(document)
                .containsEntry("username", "trader")
                .containsEntry("country", "Kenya");
    }
}
