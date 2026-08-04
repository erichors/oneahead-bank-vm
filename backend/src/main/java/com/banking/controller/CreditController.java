package com.banking.controller;

import com.banking.service.AdminService;
import com.banking.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/credit")
@CrossOrigin(origins = "*")
public class CreditController {
    
    @Autowired
    private CreditService creditService;
    
    @Autowired
    private AdminService adminService;
    
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkCredit(@RequestBody Map<String, Object> request) {
        if (adminService.is404Enabled()) {
            return ResponseEntity.notFound().build();
        }
        adminService.simulateCpuProblem();
        
        try {
            String ssn = request.get("ssn").toString();
            String metadata = (String) request.get("metadata");
            
            Map<String, Object> result = creditService.checkCredit(ssn, metadata);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
