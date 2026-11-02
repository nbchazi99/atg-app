package com.atg.api.repo;

import com.atg.api.model.Lesson;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class LessonRepository extends AbstractFirestoreStore<Lesson> {

    public LessonRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "lessons", Lesson.class);
    }

    public List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId) {
        List<Lesson> lessons = queryWhere("courseId", courseId);
        lessons.sort(Comparator.comparingInt(Lesson::getOrderIndex));
        return lessons;
    }

    public long countByCourseId(Long courseId) {
        return queryWhere("courseId", courseId).size();
    }
}
