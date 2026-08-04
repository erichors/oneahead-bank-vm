package com.banking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CreditService {

    @Autowired
    private AdminService adminService;

    @Value("${banking.credit-service-url}")
    private String creditServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> checkCredit(String ssn, String metadata) {
        if (adminService.isSlowCreditEnabled()) {
            try {
                Thread.sleep(adminService.getSlowCreditDelay() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Map<String, Object> request = new HashMap<>();
        request.put("ssn", ssn);
        request.put("metadata", metadata);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                creditServiceUrl, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Credit service unavailable: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return errorResponse;
        }
    }
}
