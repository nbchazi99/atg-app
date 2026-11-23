package com.atg.api.repo;

import com.atg.api.model.SupportTicket;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class SupportTicketRepository extends AbstractFirestoreStore<SupportTicket> {

    public SupportTicketRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "support_tickets", SupportTicket.class);
    }

    public List<SupportTicket> findByUserIdOrderByUpdatedAtDesc(Long userId) {
        List<SupportTicket> rows = queryWhere("userId", userId);
        rows.sort(Comparator.comparing(SupportTicket::getUpdatedAt).reversed());
        return rows;
    }

    public List<SupportTicket> findAllByUpdatedAtDesc() {
        List<SupportTicket> rows = findAll();
        rows.sort(Comparator.comparing(SupportTicket::getUpdatedAt).reversed());
        return rows;
    }

    public SupportTicket findByConversationId(Long conversationId) {
        return queryWhere("conversationId", conversationId).stream().findFirst().orElse(null);
    }

    public boolean belongsToUser(SupportTicket ticket, Long userId) {
        return ticket != null && Objects.equals(ticket.getUserId(), userId);
    }
}
