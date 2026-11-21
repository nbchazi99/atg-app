package com.atg.api.repo;

import com.atg.api.model.PasswordResetCode;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetCodeRepository extends AbstractFirestoreStore<PasswordResetCode> {

    public PasswordResetCodeRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "password_reset_codes", PasswordResetCode.class);
    }
}
