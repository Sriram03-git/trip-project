package com.trip.expense_splitter;

import jakarta.persistence.*; // <--- CRITICAL: Contains @Entity, @Table, etc.
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity // <--- CRITICAL FIX: Marks this class as a database table
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2) 
    private BigDecimal amount;

    // Who paid for the expense
    @ManyToOne 
    @JoinColumn(name = "paid_by_user_id", nullable = false) 
    private User paidBy;

    @Column(nullable = false)
    private LocalDate expenseDate = LocalDate.now();

    // Placeholder for simple split type (e.g., "EQUAL")
    private String splitType; 

}