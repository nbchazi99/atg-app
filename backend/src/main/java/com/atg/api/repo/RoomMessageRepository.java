package com.atg.api.repo;

import com.atg.api.model.RoomMessage;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Repository
public class RoomMessageRepository extends AbstractFirestoreStore<RoomMessage> {

    public RoomMessageRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "room_messages", RoomMessage.class);
    }

    public List<RoomMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId) {
        List<RoomMessage> messages = queryWhere("roomId", roomId);
        messages.removeIf(message -> message.getThreadId() != null);
        messages.sort(Comparator.comparing(RoomMessage::getCreatedAt));
        return messages;
    }

    public List<RoomMessage> findByRoomIdAndThreadIdOrderByCreatedAtAsc(Long roomId, Long threadId) {
        List<RoomMessage> messages = queryWhere("roomId", roomId);
        messages.removeIf(message -> !Objects.equals(message.getThreadId(), threadId));
        messages.sort(Comparator.comparing(RoomMessage::getCreatedAt));
        return messages;
    }
}
