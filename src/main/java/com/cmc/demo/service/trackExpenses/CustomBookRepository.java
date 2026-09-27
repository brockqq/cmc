package com.cmc.demo.service.trackExpenses;

import java.util.List;

import org.springframework.stereotype.Repository;
@Repository
public interface  CustomBookRepository {
	List<User> findBooksByCustomCriteria(String criteria);
}
