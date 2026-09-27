package com.cmc.demo.service.trackExpenses;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookRepositoryImpl extends CustomBookRepositoryImpl {
    @PersistenceContext
    private EntityManager entityManager;


    public List<User> findBooksByCustomCriteria2(String criteria) {
        Query query = entityManager.createQuery("SELECT b.username  FROM t_user b WHERE b.username is not null");
        return query.getResultList();
    }
}
