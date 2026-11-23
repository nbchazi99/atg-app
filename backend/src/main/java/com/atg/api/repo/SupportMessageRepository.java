package com.atg.api.repo;

import com.atg.api.model.SupportMessage;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class SupportMessageRepository extends AbstractFirestoreStore<SupportMessage> {

    public SupportMessageRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "support_messages", SupportMessage.class);
    }

    public List<SupportMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        List<SupportMessage> rows = queryWhere("conversationId", conversationId);
        rows.sort(Comparator.comparing(SupportMessage::getCreatedAt));
        return rows;
    }

    public List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId) {
        List<SupportMessage> rows = queryWhere("ticketId", ticketId);
        rows.sort(Comparator.comparing(SupportMessage::getCreatedAt));
        return rows;
    }

    public boolean belongsToConversation(SupportMessage message, Long conversationId) {
        return message != null && Objects.equals(message.getConversationId(), conversationId);
    }
}
