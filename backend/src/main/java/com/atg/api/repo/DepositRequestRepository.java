package com.atg.api.repo;

import com.atg.api.model.DepositRequest;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class DepositRequestRepository extends AbstractFirestoreStore<DepositRequest> {

    public DepositRequestRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "deposit_requests", DepositRequest.class);
    }

    public List<DepositRequest> findByUserIdOrderByCreatedAtDesc(Long userId) {
        List<DepositRequest> requests = queryWhere("userId", userId);
        requests.sort(Comparator.comparing(DepositRequest::getCreatedAt).reversed());
        return requests;
    }

    public List<DepositRequest> findAllByOrderByCreatedAtDesc() {
        List<DepositRequest> requests = findAll();
        requests.sort(Comparator.comparing(DepositRequest::getCreatedAt).reversed());
        return requests;
    }
}
