package com.atg.api.repo;

import com.atg.api.model.PurchaseRequest;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class PurchaseRequestRepository extends AbstractFirestoreStore<PurchaseRequest> {

    public PurchaseRequestRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "purchase_requests", PurchaseRequest.class);
    }

    public List<PurchaseRequest> findByUserIdOrderByCreatedAtDesc(Long userId) {
        List<PurchaseRequest> requests = queryWhere("userId", userId);
        requests.sort(Comparator.comparing(PurchaseRequest::getCreatedAt).reversed());
        return requests;
    }

    public List<PurchaseRequest> findAllByOrderByCreatedAtDesc() {
        List<PurchaseRequest> requests = findAll();
        requests.sort(Comparator.comparing(PurchaseRequest::getCreatedAt).reversed());
        return requests;
    }
}
