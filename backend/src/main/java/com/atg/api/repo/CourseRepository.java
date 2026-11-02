package com.atg.api.repo;

import com.atg.api.model.Course;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class CourseRepository extends AbstractFirestoreStore<Course> {

    public CourseRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "courses", Course.class);
    }
}
