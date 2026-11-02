package com.atg.api.repo;

import com.atg.api.model.User;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository extends AbstractFirestoreStore<User> {

    public UserRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "users", User.class);
    }

    public Optional<User> findByEmailIgnoreCase(String email) {
        if (email == null) return Optional.empty();
        List<User> matches = queryWhere("email", email.toLowerCase());
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public boolean existsByEmailIgnoreCase(String email) {
        return findByEmailIgnoreCase(email).isPresent();
    }
}
