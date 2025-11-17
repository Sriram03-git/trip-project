package com.trip.expense_splitter.controller;

import com.trip.expense_splitter.Expense;
import com.trip.expense_splitter.repository.ExpenseRepository;
import com.trip.expense_splitter.repository.UserRepository;
import com.trip.expense_splitter.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Primary Constructor Injection
    public ExpenseController(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }
    
    // Method: POST /api/expenses
    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        // 1. Basic check for missing ID
        if (expense.getPaidBy() == null || expense.getPaidBy().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long paidById = expense.getPaidBy().getId();

        // 2. Check if the User exists before saving the expense
        Optional<User> paidByUser = userRepository.findById(paidById);
       
        if (paidByUser.isPresent()) {
            expense.setPaidBy(paidByUser.get()); // Attach the full User object
            Expense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.ok(savedExpense);
        } else {
            return ResponseEntity.badRequest().build(); // User not found
        }
    }

    // Method: GET /api/expenses
    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
}