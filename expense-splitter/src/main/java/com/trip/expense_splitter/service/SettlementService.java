package com.trip.expense_splitter.service;

import com.trip.expense_splitter.Expense;
import com.trip.expense_splitter.ExpenseType; // Import the new Enum
import com.trip.expense_splitter.User;
import com.trip.expense_splitter.repository.ExpenseRepository;
import com.trip.expense_splitter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public SettlementService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. Core logic for debt simplification (Only considers GROUP expenses)
    // =========================================================================

    public List<Settlement> calculateSettlements() {
        // 1. Calculate Net Balance for all users
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        List<User> users = userRepository.findAll();
        users.forEach(user -> netBalances.put(user.getId(), BigDecimal.ZERO));

        List<Expense> expenses = expenseRepository.findAll();
        
        for (Expense expense : expenses) {
            
            // --- CRITICAL FIX: Only process GROUP expenses for splitting ---
            if (expense.getExpenseType() != ExpenseType.GROUP) {
                continue; 
            }
            
            BigDecimal totalAmount = expense.getAmount();
            int userCount = users.size(); 
            
            if (userCount == 0) continue;

            BigDecimal share = totalAmount.divide(BigDecimal.valueOf(userCount), 2, RoundingMode.HALF_UP);

            // Update the payer's balance (Total paid - their own share)
            Long paidById = expense.getPaidBy().getId();
            netBalances.put(paidById, netBalances.get(paidById).add(totalAmount.subtract(share)));

            // Update the debtors' balance (subtract the share)
            for (User user : users) {
                // If a user is not included in the expense, they aren't part of the split.
                // Assuming all registered users are part of the split for now.
                // If specific users are involved, the logic needs further refinement.
                if (!user.getId().equals(paidById)) {
                    netBalances.put(user.getId(), netBalances.get(user.getId()).subtract(share));
                }
            }
        }

        // 2. Debt Simplification Algorithm (Minimizing Transactions) - REMAINS THE SAME
        PriorityQueue<Transaction> givers = new PriorityQueue<>((a, b) -> a.getAmount().compareTo(b.getAmount()));
        PriorityQueue<Transaction> takers = new PriorityQueue<>((a, b) -> b.getAmount().compareTo(a.getAmount()));

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) { // Net negative (Owes)
                givers.add(new Transaction(entry.getKey(), entry.getValue().abs()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) { // Net positive (Receives)
                takers.add(new Transaction(entry.getKey(), entry.getValue()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();
        BigDecimal smallTolerance = new BigDecimal("0.01"); 

        while (!givers.isEmpty() && !takers.isEmpty()) {
            Transaction giver = givers.poll();
            Transaction taker = takers.poll();

            BigDecimal settlementAmount = giver.getAmount().min(taker.getAmount()).setScale(2, RoundingMode.HALF_UP);

            if (settlementAmount.compareTo(smallTolerance) < 0) continue; // Skip near-zero settlements

            settlements.add(new Settlement(giver.getUserId(), taker.getUserId(), settlementAmount));

            BigDecimal remainingGiver = giver.getAmount().subtract(settlementAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingTaker = taker.getAmount().subtract(settlementAmount).setScale(2, RoundingMode.HALF_UP);

            if (remainingGiver.compareTo(smallTolerance) >= 0) { 
                giver.setAmount(remainingGiver); 
                givers.add(giver);
            }
            if (remainingTaker.compareTo(smallTolerance) >= 0) {
                taker.setAmount(remainingTaker);
                takers.add(taker);
            }
        }
        
        return settlements;
    }

    // =========================================================================
    // 2. Category Spending Breakdown Logic
    // =========================================================================

    public List<CategorySpending> getCategorySpending() {
        List<Expense> expenses = expenseRepository.findAll();
        
        // Map: Category -> UserID -> TotalSpentByThatUserInThatCategory
        Map<String, Map<Long, BigDecimal>> categoryUserSpending = new HashMap<>();

        for (Expense expense : expenses) {
            String category = expense.getCategory();
            if (category == null || category.trim().isEmpty()) {
                continue; // Skip expenses without a valid category
            }
            
            Long paidById = expense.getPaidBy().getId();
            BigDecimal amount = expense.getAmount();

            categoryUserSpending
                .computeIfAbsent(category.toUpperCase().trim(), k -> new HashMap<>())
                .merge(paidById, amount, BigDecimal::add); // Add amount to existing total
        }

        List<CategorySpending> results = new ArrayList<>();

        for (Map.Entry<String, Map<Long, BigDecimal>> catEntry : categoryUserSpending.entrySet()) {
            String category = catEntry.getKey();
            
            // Find the top spender for this category
            Map.Entry<Long, BigDecimal> topSpender = catEntry.getValue().entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .orElse(null);

            if (topSpender != null) {
                results.add(new CategorySpending(
                    category, 
                    topSpender.getKey(), 
                    topSpender.getValue(),
                    catEntry.getValue() // Include all user spending for this category
                ));
            }
        }
        
        return results;
    }


    // Internal Helper Classes (Transaction remains the same)
    
    private static class Transaction {
        private Long userId;
        private BigDecimal amount;

        public Transaction(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        public Long getUserId() { return userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    // Output model for Settlement API (Settlement remains the same)
    public static class Settlement {
        private Long giverId;
        private Long receiverId;
        private BigDecimal amount;
        
        public Long getOwesUserId() { return giverId; }
        public Long getReceivesUserId() { return receiverId; }
        public BigDecimal getAmount() { return amount; }

        public Settlement(Long giverId, Long receiverId, BigDecimal amount) {
            this.giverId = giverId;
            this.receiverId = receiverId;
            this.amount = amount;
        }
    }
    
    // Output model for the new Category Spending Breakdown API
    public static class CategorySpending {
        private String category;
        private Long topSpenderId;
        private BigDecimal totalCategorySpend;
        // Map<UserId, AmountSpent> - detailed breakdown for all users
        private Map<Long, BigDecimal> userSpendingBreakdown; 

        public CategorySpending(String category, Long topSpenderId, BigDecimal totalCategorySpend, Map<Long, BigDecimal> userSpendingBreakdown) {
            this.category = category;
            this.topSpenderId = topSpenderId;
            this.totalCategorySpend = totalCategorySpend;
            this.userSpendingBreakdown = userSpendingBreakdown;
        }
        
        public String getCategory() { return category; }
        public Long getTopSpenderId() { return topSpenderId; }
        public BigDecimal getTotalCategorySpend() { return totalCategorySpend; }
        public Map<Long, BigDecimal> getUserSpendingBreakdown() { return userSpendingBreakdown; }
    }
}