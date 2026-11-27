package com.atg.api.repo;

import com.atg.api.model.PaymentTransaction;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentTransactionRepository extends AbstractFirestoreStore<PaymentTransaction> {

    public PaymentTransactionRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "payment_transactions", PaymentTransaction.class);
    }

    public Optional<PaymentTransaction> findByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) return Optional.empty();
        return queryWhere("orderId", orderId).stream().findFirst();
    }

    public List<PaymentTransaction> findByUserId(Long userId) {
        return queryWhere("userId", userId);
    }

    public List<PaymentTransaction> findByStatus(PaymentTransaction.Status status) {
        return queryWhere("status", status.name());
    }
}
