package com.atg.api.repo;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class SupportKnowledgeRepository extends AbstractFirestoreStore<SupportKnowledgeEntry> {

    public SupportKnowledgeRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "support_knowledge_base", SupportKnowledgeEntry.class);
    }

    public List<SupportKnowledgeEntry> findActiveOrderByTitle() {
        List<SupportKnowledgeEntry> rows = queryWhere("active", true);
        rows.sort(Comparator.comparing(
                row -> row.getTitle() == null ? "" : row.getTitle(),
                String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    public SupportKnowledgeEntry findByTitle(String title) {
        return queryWhere("title", title).stream().findFirst().orElse(null);
    }
}
