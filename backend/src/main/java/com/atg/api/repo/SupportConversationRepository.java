package com.atg.api.repo;

import com.atg.api.model.SupportConversation;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class SupportConversationRepository extends AbstractFirestoreStore<SupportConversation> {

    public SupportConversationRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "support_conversations", SupportConversation.class);
    }

    public List<SupportConversation> findByUserIdOrderByUpdatedAtDesc(Long userId) {
        List<SupportConversation> rows = queryWhere("userId", userId);
        rows.sort(Comparator.comparing(SupportConversation::getUpdatedAt).reversed());
        return rows;
    }

    public boolean belongsToUser(SupportConversation conversation, Long userId) {
        return conversation != null && Objects.equals(conversation.getUserId(), userId);
    }
}
