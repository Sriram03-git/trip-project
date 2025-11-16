package com.trip.expense_splitter.service;

import com.trip.expense_splitter.Expense;
import com.trip.expense_splitter.User;
import com.trip.expense_splitter.repository.ExpenseRepository;
import com.trip.expense_splitter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; // Corrected Import for modern division
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Spring uses this constructor for dependency injection (Best Practice)
    public SettlementService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    // கடன் சுலபமாக்கும் அல்காரிதம் கோர் லாஜிக்
    public List<Settlement> calculateSettlements() {
        // 1. பயனர்களின் நிகர இருப்பைக் கணக்கிடுதல் (Calculate Net Balance for all users)
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        List<User> users = userRepository.findAll();
        users.forEach(user -> netBalances.put(user.getId(), BigDecimal.ZERO));

        List<Expense> expenses = expenseRepository.findAll();
        
        for (Expense expense : expenses) {
            BigDecimal totalAmount = expense.getAmount();
            
            // Fetch all unique users involved in any transaction to determine the split group size
            int userCount = users.size(); 
            
            if (userCount == 0) continue;

            // ஒவ்வொரு பயனரும் செலுத்த வேண்டிய பங்கு (Fixed divide method)
            BigDecimal share = totalAmount.divide(BigDecimal.valueOf(userCount), 2, RoundingMode.HALF_UP);

            // பணம் செலுத்தியவர் வரவு (+ve) பெறுகிறார்
            Long paidById = expense.getPaidBy().getId();
            
            // Update the payer's balance (Total paid - their own share)
            netBalances.put(paidById, netBalances.get(paidById).add(totalAmount.subtract(share)));

            // மற்ற அனைவரும் கடன் (-ve) படுகிறார்கள்
            for (User user : users) {
                if (!user.getId().equals(paidById)) {
                    netBalances.put(user.getId(), netBalances.get(user.getId()).subtract(share));
                }
            }
        }

        // 2. கடன் சுலபமாக்கல் அல்காரிதம் (Minimizing Transactions using Priority Queues)

        // Givers (Owe money) - Sorted ascending (smallest negative first)
        PriorityQueue<Transaction> givers = new PriorityQueue<>((a, b) -> a.getAmount().compareTo(b.getAmount()));
        // Takers (Receive money) - Sorted descending (largest positive first)
        PriorityQueue<Transaction> takers = new PriorityQueue<>((a, b) -> b.getAmount().compareTo(a.getAmount()));

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                givers.add(new Transaction(entry.getKey(), entry.getValue().abs()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) { 
                takers.add(new Transaction(entry.getKey(), entry.getValue()));
            }
        }

        List<Settlement> settlements = new ArrayList<>();

        while (!givers.isEmpty() && !takers.isEmpty()) {
            Transaction giver = givers.poll();
            Transaction taker = takers.poll();

            BigDecimal settlementAmount = giver.getAmount().min(taker.getAmount());

            
            settlements.add(new Settlement(giver.getUserId(), taker.getUserId(), settlementAmount));

            giver.setAmount(giver.getAmount().subtract(settlementAmount));
            taker.setAmount(taker.getAmount().subtract(settlementAmount));

            if (giver.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                givers.add(giver);
            }
            if (taker.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                takers.add(taker);
            }
        }
        
        return settlements;
    }

    // =================================================================================
    //  (Internal Helper Classes - MUST be static if defined inside)
    // =================================================================================
    
    
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

    // (Final Output - Public static so it can be used in the Controller)
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