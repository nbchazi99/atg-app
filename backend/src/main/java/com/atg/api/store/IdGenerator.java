package com.atg.api.store;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Allocates numeric document ids per collection via a counters/{collection} document. */
@Component
public class IdGenerator {

    private final Firestore db;

    public IdGenerator(Firestore db) {
        this.db = db;
    }

    public long next(String collection) {
        DocumentReference counter = db.collection("counters").document(collection);
        try {
            return db.runTransaction(transaction -> {
                DocumentSnapshot snap = transaction.get(counter).get();
                long next = snap.exists() ? (long) snap.getLong("next") : 1L;
                transaction.set(counter, Map.of("next", next + 1));
                return next;
            }).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate id for " + collection, e);
        }
    }
}
