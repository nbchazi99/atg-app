package com.atg.api.store;

import java.util.List;
import java.util.Optional;

/** CRUD surface every entity store provides. */
public interface BaseStore<T> {

    T save(T entity);

    Optional<T> findById(Long id);

    List<T> findAll();

    void delete(T entity);

    void deleteById(Long id);

    long count();
}
