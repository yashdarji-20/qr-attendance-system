package com.attendance.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface providing standard CRUD operations.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public interface GenericDAO<T, ID> {

    /** Persist a new entity and return the generated ID. */
    int insert(T entity) throws SQLException;

    /** Update an existing entity. Returns rows affected. */
    int update(T entity) throws SQLException;

    /** Soft- or hard-delete an entity by ID. Returns rows affected. */
    int delete(ID id) throws SQLException;

    /** Find entity by primary key. */
    Optional<T> findById(ID id) throws SQLException;

    /** Retrieve all entities. */
    List<T> findAll() throws SQLException;
}
