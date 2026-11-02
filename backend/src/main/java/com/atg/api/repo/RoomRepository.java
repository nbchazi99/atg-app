package com.atg.api.repo;

import com.atg.api.model.Room;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository extends AbstractFirestoreStore<Room> {

    public RoomRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "rooms", Room.class);
    }
}
