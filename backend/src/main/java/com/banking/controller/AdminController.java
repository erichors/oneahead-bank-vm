package com.banking.controller;

import com.banking.model.AdminConfig;
import com.banking.model.User;
import com.banking.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @GetMapping("/configs")
    public ResponseEntity<List<AdminConfig>> getAllConfigs() {
        return ResponseEntity.ok(adminService.getAllConfigs());
    }
    
    @GetMapping("/configs/{key}")
    public ResponseEntity<AdminConfig> getConfig(@PathVariable String key) {
        return adminService.getConfig(key)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/configs")
    public ResponseEntity<AdminConfig> updateConfig(@RequestBody Map<String, Object> request) {
        String key = request.get("key").toString();
        String value = request.get("value").toString();
        String description = (String) request.get("description");
        
        AdminConfig config = adminService.updateConfig(key, value, description);
        return ResponseEntity.ok(config);
    }
    
    @PostMapping("/traffic")
    public ResponseEntity<Map<String, Object>> toggleTraffic(@RequestBody Map<String, Object> request) {
        boolean enabled = (Boolean) request.get("enabled");
        adminService.updateConfig("traffic.enabled", String.valueOf(enabled), "Traffic simulation");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Traffic simulation " + (enabled ? "enabled" : "disabled"));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/errors/404")
    public ResponseEntity<Map<String, Object>> toggle404(@RequestBody Map<String, Object> request) {
        boolean enabled = (Boolean) request.get("enabled");
        adminService.updateConfig("error.404.enabled", String.valueOf(enabled), "404 error simulation");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "404 error simulation " + (enabled ? "enabled" : "disabled"));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sql/slow")
    public ResponseEntity<Map<String, Object>> toggleSlowSql(@RequestBody Map<String, Object> request) {
        boolean enabled = (Boolean) request.get("enabled");
        int delay = (Integer) request.get("delay");
        adminService.updateConfig("sql.slow.enabled", String.valueOf(enabled), "Slow SQL simulation");
        adminService.updateConfig("sql.slow.delay", String.valueOf(delay), "Slow SQL delay in seconds");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Slow SQL simulation " + (enabled ? "enabled" : "disabled") + " with " + delay + "s delay");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/credit/slow")
    public ResponseEntity<Map<String, Object>> toggleSlowCredit(@RequestBody Map<String, Object> request) {
        boolean enabled = (Boolean) request.get("enabled");
        int delay = (Integer) request.get("delay");
        adminService.updateConfig("credit.slow.enabled", String.valueOf(enabled), "Slow credit check simulation");
        adminService.updateConfig("credit.slow.delay", String.valueOf(delay), "Slow credit check delay in seconds");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Slow credit check simulation " + (enabled ? "enabled" : "disabled") + " with " + delay + "s delay");
        return ResponseEntity.ok(response);
    }
    
    // User Management Endpoints
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
    
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        try {
            String username = request.get("username").toString();
            String password = request.get("password").toString();
            String firstName = request.get("firstName").toString();
            String lastName = request.get("lastName").toString();
            
            User user = adminService.createUser(username, password, firstName, lastName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/users/default")
    public ResponseEntity<Map<String, Object>> createDefaultUsers() {
        try {
            adminService.createDefaultUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Default users created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create default users: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
