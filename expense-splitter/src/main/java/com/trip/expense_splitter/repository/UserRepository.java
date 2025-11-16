package com.trip.expense_splitter.repository;

import com.trip.expense_splitter.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA provides basic CRUD methods automatically
}