package com.trip.expense_splitter.service;

import com.trip.expense_splitter.Expense;
import com.trip.expense_splitter.User;
import com.trip.expense_splitter.repository.ExpenseRepository;
import com.trip.expense_splitter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; // Necessary for safe division
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Constructor Injection (Cleanest way to manage dependencies)
    public SettlementService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    // Core logic for debt simplification
    public List<Settlement> calculateSettlements() {
        // 1. Calculate Net Balance for all users
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        List<User> users = userRepository.findAll();
        users.forEach(user -> netBalances.put(user.getId(), BigDecimal.ZERO));

        List<Expense> expenses = expenseRepository.findAll();
        
        for (Expense expense : expenses) {
            BigDecimal totalAmount = expense.getAmount();
            int userCount = users.size(); 
            
            if (userCount == 0) continue;

            // Share calculation (Fixed deprecated method)
            BigDecimal share = totalAmount.divide(BigDecimal.valueOf(userCount), 2, RoundingMode.HALF_UP);

            // Update the payer's balance (Total paid - their own share)
            Long paidById = expense.getPaidBy().getId();
            netBalances.put(paidById, netBalances.get(paidById).add(totalAmount.subtract(share)));

            // Update the debtors' balance (subtract the share)
            for (User user : users) {
                if (!user.getId().equals(paidById)) {
                    netBalances.put(user.getId(), netBalances.get(user.getId()).subtract(share));
                }
            }
        }

        // 2. Debt Simplification Algorithm (Minimizing Transactions)

        // Givers (Owe money) - Sorted ascending
        PriorityQueue<Transaction> givers = new PriorityQueue<>((a, b) -> a.getAmount().compareTo(b.getAmount()));
        // Takers (Receive money) - Sorted descending
        PriorityQueue<Transaction> takers = new PriorityQueue<>((a, b) -> b.getAmount().compareTo(a.getAmount()));

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) { // Net negative (Owes)
                givers.add(new Transaction(entry.getKey(), entry.getValue().abs()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) { // Net positive (Receives)
                takers.add(new Transaction(entry.getKey(), entry.getValue()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();
        BigDecimal smallTolerance = new BigDecimal("0.01"); // Tolerance for cleanup

        while (!givers.isEmpty() && !takers.isEmpty()) {
            Transaction giver = givers.poll();
            Transaction taker = takers.poll();

            BigDecimal settlementAmount = giver.getAmount().min(taker.getAmount());

            settlements.add(new Settlement(giver.getUserId(), taker.getUserId(), settlementAmount));

            // Subtract and round remaining amounts (Fix for infinite loop)
            BigDecimal remainingGiver = giver.getAmount().subtract(settlementAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal remainingTaker = taker.getAmount().subtract(settlementAmount).setScale(2, RoundingMode.HALF_UP);

            // Re-add to queue if the remaining amount is significant
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

    // Internal Helper Classes
    
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

    // Output model for the API
    public static class Settlement {
        private Long giverId;
        private Long receiverId;
        private BigDecimal amount;
        
        // Simpler names for JSON output (used by Jackson serializer)
        public Long getOwesUserId() { return giverId; }
        public Long getReceivesUserId() { return receiverId; }
        public BigDecimal getAmount() { return amount; }

        public Settlement(Long giverId, Long receiverId, BigDecimal amount) {
            this.giverId = giverId;
            this.receiverId = receiverId;
            this.amount = amount;
        }
    }
}