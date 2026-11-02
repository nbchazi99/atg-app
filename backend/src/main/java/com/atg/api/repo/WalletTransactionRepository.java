package com.atg.api.repo;

import com.atg.api.model.WalletTransaction;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class WalletTransactionRepository extends AbstractFirestoreStore<WalletTransaction> {

    public WalletTransactionRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "wallet_transactions", WalletTransaction.class);
    }

    public List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId) {
        List<WalletTransaction> transactions = queryWhere("userId", userId);
        transactions.sort(Comparator.comparing(WalletTransaction::getCreatedAt).reversed());
        return transactions;
    }
}
