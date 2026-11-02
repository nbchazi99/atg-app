package com.atg.api.repo;

import com.atg.api.model.PushToken;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PushTokenRepository extends AbstractFirestoreStore<PushToken> {

    public PushTokenRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "push_tokens", PushToken.class);
    }

    public List<PushToken> findByUserId(Long userId) {
        return queryWhere("userId", userId);
    }

    public void deleteByToken(String token) {
        for (PushToken pushToken : queryWhere("token", token)) {
            delete(pushToken);
        }
    }
}
