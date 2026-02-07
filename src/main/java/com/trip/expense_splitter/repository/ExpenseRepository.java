package com.trip.expense_splitter.repository;

import com.trip.expense_splitter.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // Spring Data JPA provides basic CRUD methods automatically
}