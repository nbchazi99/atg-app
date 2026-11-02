package com.atg.api.repo;

import com.atg.api.model.AppSettings;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

@Repository
public class AppSettingsRepository extends AbstractFirestoreStore<AppSettings> {

    public AppSettingsRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "app_settings", AppSettings.class);
    }
}
