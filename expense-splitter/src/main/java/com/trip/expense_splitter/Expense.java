package com.trip.expense_splitter;

import jakarta.persistence.*; 
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity 
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

    // --- புதிய ஃபீல்டுகள் ---
    
    // Defines if the expense is 'PERSONAL' (not split) or 'GROUP' (to be split)
    @Enumerated(EnumType.STRING) // Saves the enum as a String in the DB
    @Column(nullable = false)
    private ExpenseType expenseType = ExpenseType.GROUP; // Default to GROUP

    // Category for breakdown (e.g., 'FOOD', 'FUEL')
    @Column(nullable = true)
    private String category;
    
    // ----------------------
    
    // Old placeholder removed: private String splitType; 
}