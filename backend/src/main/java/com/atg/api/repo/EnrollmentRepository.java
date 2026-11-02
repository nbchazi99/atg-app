package com.atg.api.repo;

import com.atg.api.model.Enrollment;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentRepository extends AbstractFirestoreStore<Enrollment> {

    public EnrollmentRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "enrollments", Enrollment.class);
    }

    public Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId) {
        return queryWhere("userId", userId).stream()
                .filter(e -> courseId.equals(e.getCourseId()))
                .findFirst();
    }

    public List<Enrollment> findByUserId(Long userId) {
        return queryWhere("userId", userId);
    }
}
