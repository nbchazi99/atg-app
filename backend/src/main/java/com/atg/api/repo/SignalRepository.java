package com.atg.api.repo;

import com.atg.api.model.Signal;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class SignalRepository extends AbstractFirestoreStore<Signal> {

    public SignalRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "signals", Signal.class);
    }

    public List<Signal> findByStatusOrderByCreatedAtDesc(Signal.Status status) {
        List<Signal> signals = queryWhere("status", status.name());
        signals.sort(Comparator.comparing(Signal::getCreatedAt).reversed());
        return signals;
    }

    public List<Signal> findBySymbolOrderByCreatedAtDesc(String symbol) {
        List<Signal> signals = queryWhere("symbol", symbol.toUpperCase());
        signals.sort(Comparator.comparing(Signal::getCreatedAt).reversed());
        return signals;
    }

    public long countByStatus(Signal.Status status) {
        return queryWhere("status", status.name()).size();
    }

    public List<Signal> findAllByOrderByCreatedAtDesc() {
        List<Signal> signals = findAll();
        signals.sort(Comparator.comparing(Signal::getCreatedAt).reversed());
        return signals;
    }
}
