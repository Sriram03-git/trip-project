package com.trip.expense_splitter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trip.expense_splitter.service.SettlementService;

@RestController
@RequestMapping("/api/settlements") 
@CrossOrigin(origins = "*") // Allow requests from any origin
public class SettlementController {

    private final SettlementService settlementService;

    // Constructor Injection (Resolves dependency on the service layer)
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