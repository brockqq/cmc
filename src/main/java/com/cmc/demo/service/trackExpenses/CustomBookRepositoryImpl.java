package com.cmc.demo.service.trackExpenses;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CustomBookRepositoryImpl implements CustomBookRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<User> findBooksByCustomCriteria(String criteria) {
        Query query = entityManager.createQuery("SELECT b.username  FROM tuser b WHERE b.username is not null");
        return query.getResultList();
    }
}
