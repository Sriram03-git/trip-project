package com.trip.expense_splitter.controller;

import com.trip.expense_splitter.service.SettlementService; // Necessary for calculation logic
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements") 
public class SettlementController {

    // Final reference to the service layer
    private final SettlementService settlementService;

    // Constructor Injection
    @Autowired
    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    // API to calculate and return settlements
    // Method: GET /api/settlements
    @GetMapping
    public List<SettlementService.Settlement> getSettlements() {
        return settlementService.calculateSettlements();
    }
}