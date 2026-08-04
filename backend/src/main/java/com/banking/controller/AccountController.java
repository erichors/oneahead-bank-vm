package com.banking.controller;

import com.banking.service.AdminService;
import com.banking.service.BankingService;
import com.banking.model.TransferResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {
    
    @Autowired
    private BankingService bankingService;
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {
        if (adminService.is404Enabled()) {
            return ResponseEntity.notFound().build();
        }
        adminService.simulateCpuProblem();
        
        try {
            BigDecimal balance = bankingService.getBalance();
            Map<String, Object> response = new HashMap<>();
            response.put("balance", balance);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> request) {
        if (adminService.is404Enabled()) {
            return ResponseEntity.notFound().build();
        }
        adminService.simulateCpuProblem();
        
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String metadata = (String) request.get("metadata");
            
            bankingService.deposit(amount, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Deposit successful");
            response.put("amount", amount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> request) {
        if (adminService.is404Enabled()) {
            return ResponseEntity.notFound().build();
        }
        adminService.simulateCpuProblem();
        
        try {
            String toAccount = request.get("toAccount").toString();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String metadata = (String) request.get("metadata");
            
            TransferResult result = bankingService.transfer(toAccount, amount, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transfer successful");
            response.put("transactionId", result.getTransactionId());
            response.put("fromAccount", result.getFromAccount());
            response.put("toAccount", result.getToAccount());
            response.put("amount", result.getAmount());
            response.put("fromBalanceBefore", result.getFromBalanceBefore());
            response.put("fromBalanceAfter", result.getFromBalanceAfter());
            response.put("toBalanceBefore", result.getToBalanceBefore());
            response.put("toBalanceAfter", result.getToBalanceAfter());
            response.put("timestamp", result.getTimestamp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
