package com.banking.credit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/credit")
public class CreditController {
    private CreditConfig config = new CreditConfig(false, 5);

    @PostMapping("/check")
    public CreditCheckResponse checkCredit(@RequestBody CreditCheckRequest request) throws InterruptedException {
        long start = System.currentTimeMillis();
        String ssn = request.ssn() == null ? "" : request.ssn();

        if (config.slowEnabled()) {
            Thread.sleep(config.delay() * 1000L);
        }

        int score = generateCreditScore(ssn);
        Thread.sleep(new Random(ssn.hashCode()).nextInt(150, 900));

        return new CreditCheckResponse(
            score,
            getCreditStatus(score),
            System.currentTimeMillis() - start,
            request.metadata(),
            generateCreditHistory(ssn, score)
        );
    }

    @GetMapping("/admin/config")
    public CreditConfig getConfig() {
        return config;
    }

    @PostMapping("/admin/config")
    public CreditConfig updateConfig(@RequestBody CreditConfig newConfig) {
        config = newConfig;
        return config;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "tier", "credit-service");
    }

    private int generateCreditScore(String ssn) {
        return 300 + (Math.abs(ssn.hashCode()) % 551);
    }

    private String getCreditStatus(int score) {
        if (score >= 750) return "EXCELLENT";
        if (score >= 700) return "GOOD";
        if (score >= 650) return "FAIR";
        if (score >= 600) return "POOR";
        return "VERY_POOR";
    }

    private List<CreditHistory> generateCreditHistory(String ssn, int currentScore) {
        int seed = Math.abs(ssn.hashCode());
        int baseScore = currentScore - (seed % 100) - 50;
        LocalDate start = LocalDate.now().minus(23, ChronoUnit.MONTHS);
        List<CreditHistory> history = new ArrayList<>();
        int previous = Math.max(300, Math.min(850, baseScore));

        for (int month = 0; month < 24; month++) {
            int score = Math.max(300, Math.min(850, baseScore + (month * 4) + (seed % 20) - 10));
            int change = month == 0 ? 0 : score - previous;
            history.add(new CreditHistory(start.plus(month, ChronoUnit.MONTHS).toString(), score, change, describe(change, score)));
            previous = score;
        }

        return history;
    }

    private String describe(int change, int score) {
        if (change > 0) return "Credit score improved due to on-time payments";
        if (change < 0) return "Credit score decreased due to late payment";
        if (score >= 750) return "Maintained excellent credit standing";
        if (score >= 700) return "Maintained good credit standing";
        if (score >= 650) return "Credit score remained stable";
        return "Working to improve credit standing";
    }

    public record CreditCheckRequest(String ssn, String metadata) {}
    public record CreditCheckResponse(int creditScore, String status, long responseTime, String metadata, List<CreditHistory> history) {}
    public record CreditHistory(String date, int score, int change, String description) {}
    public record CreditConfig(boolean slowEnabled, int delay) {}
}
