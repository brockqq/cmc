package com.cmc.demo.service.trackExpenses;

import org.springframework.data.jpa.repository.JpaRepository;


public interface  BookRepository extends JpaRepository<User, Long>, CustomBookRepository {

}
