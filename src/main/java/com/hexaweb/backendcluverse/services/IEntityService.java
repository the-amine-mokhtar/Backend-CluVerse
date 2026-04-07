package com.hexaweb.backendcluverse.services;

import java.util.List;
import java.util.Optional;

public interface IEntityService<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    T save(T entity);
    void deleteById(ID id);
}

