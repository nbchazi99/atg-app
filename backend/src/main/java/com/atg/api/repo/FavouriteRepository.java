package com.atg.api.repo;

import com.atg.api.model.Favourite;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FavouriteRepository extends AbstractFirestoreStore<Favourite> {

    public FavouriteRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "favourites", Favourite.class);
    }

    public List<Favourite> findByUserId(Long userId) {
        return queryWhere("userId", userId);
    }

    public Optional<Favourite> findByUserIdAndSymbol(Long userId, String symbol) {
        return queryWhere("userId", userId).stream()
                .filter(f -> symbol.equalsIgnoreCase(f.getSymbol()))
                .findFirst();
    }
}
