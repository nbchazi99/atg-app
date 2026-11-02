package com.atg.api.store;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class AbstractFirestoreStore<T> implements BaseStore<T> {

    protected final Firestore db;
    protected final IdGenerator ids;
    protected final String collection;
    protected final Class<T> type;

    protected AbstractFirestoreStore(Firestore db, IdGenerator ids, String collection, Class<T> type) {
        this.db = db;
        this.ids = ids;
        this.collection = collection;
        this.type = type;
    }

    @Override
    public T save(T entity) {
        Long id = getId(entity);
        if (id == null) {
            id = ids.next(collection);
            setId(entity, id);
        }
        await(db.collection(collection).document(String.valueOf(id)).set(EntityMapper.toDoc(entity)));
        return entity;
    }

    @Override
    public Optional<T> findById(Long id) {
        if (id == null) return Optional.empty();
        DocumentSnapshot snap = await(db.collection(collection).document(String.valueOf(id)).get());
        return snap.exists() ? Optional.of(EntityMapper.fromDoc(type, snap)) : Optional.empty();
    }

    @Override
    public List<T> findAll() {
        QuerySnapshot snap = await(db.collection(collection).get());
        return toList(snap);
    }

    @Override
    public void delete(T entity) {
        Long id = getId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        await(db.collection(collection).document(String.valueOf(id)).delete());
    }

    @Override
    public long count() {
        QuerySnapshot snap = await(db.collection(collection).get());
        return snap.size();
    }

    protected List<T> queryWhere(String field, Object value) {
        QuerySnapshot snap = await(db.collection(collection).whereEqualTo(field, value).get());
        return toList(snap);
    }

    protected List<T> query(Query query) {
        return toList(await(query.get()));
    }

    private List<T> toList(QuerySnapshot snap) {
        List<T> result = new ArrayList<>(snap.size());
        for (DocumentSnapshot doc : snap.getDocuments()) {
            result.add(EntityMapper.fromDoc(type, doc));
        }
        return result;
    }

    protected static <R> R await(ApiFuture<R> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore operation interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore operation failed", e.getCause());
        }
    }

    private Long getId(T entity) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return (Long) field.get(entity);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Entity has no id field: " + entity.getClass().getName(), e);
        }
    }

    private void setId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Entity has no id field: " + entity.getClass().getName(), e);
        }
    }
}
