package com.trip.expense_splitter.repository;

import com.trip.expense_splitter.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}