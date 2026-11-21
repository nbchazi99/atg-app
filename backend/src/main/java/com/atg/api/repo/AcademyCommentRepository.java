package com.atg.api.repo;

import com.atg.api.model.AcademyComment;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class AcademyCommentRepository extends AbstractFirestoreStore<AcademyComment> {

    public AcademyCommentRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "academy_comments", AcademyComment.class);
    }

    public List<AcademyComment> findByCourseIdAndLessonId(Long courseId, Long lessonId) {
        List<AcademyComment> comments = queryWhere("courseId", courseId);
        if (lessonId == null) {
            comments.removeIf(comment -> comment.getLessonId() != null);
        } else {
            comments.removeIf(comment -> !Objects.equals(comment.getLessonId(), lessonId));
        }
        comments.sort(Comparator.comparing(AcademyComment::getCreatedAt));
        return comments;
    }
}
