package com.trip.expense_splitter.controller;

import com.trip.expense_splitter.service.SettlementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spending-breakdown")
public class SpendingBreakdownController {

    private final SettlementService settlementService;

    public SpendingBreakdownController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    // API to calculate and return category-wise spending
    // Method: GET /api/spending-breakdown
    @GetMapping
    public List<SettlementService.CategorySpending> getCategorySpending() {
        return settlementService.getCategorySpending();
    }
}