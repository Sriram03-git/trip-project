package com.trip.expense_splitter;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private BigDecimal amount;
    private String category;

    @ManyToOne
    @JoinColumn(name = "paid_by_user_id")
    private User paidBy;

    @Enumerated(EnumType.STRING)
    private ExpenseType expenseType;

    private LocalDate expenseDate;

    @PrePersist
    protected void onCreate() {
        expenseDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public User getPaidBy() { return paidBy; }
    public void setPaidBy(User paidBy) { this.paidBy = paidBy; }
    public ExpenseType getExpenseType() { return expenseType; }
    public void setExpenseType(ExpenseType expenseType) { this.expenseType = expenseType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
}