package com.atg.api.repo;

import com.atg.api.model.WithdrawalRequest;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class WithdrawalRequestRepository extends AbstractFirestoreStore<WithdrawalRequest> {

    public WithdrawalRequestRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "withdrawal_requests", WithdrawalRequest.class);
    }

    public List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId) {
        List<WithdrawalRequest> requests = queryWhere("userId", userId);
        requests.sort(Comparator.comparing(WithdrawalRequest::getCreatedAt).reversed());
        return requests;
    }

    public List<WithdrawalRequest> findAllByOrderByCreatedAtDesc() {
        List<WithdrawalRequest> requests = findAll();
        requests.sort(Comparator.comparing(WithdrawalRequest::getCreatedAt).reversed());
        return requests;
    }
}
