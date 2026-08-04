package com.banking.service;

import com.banking.model.Account;
import com.banking.model.AdminConfig;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.AdminConfigRepository;
import com.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminConfigRepository adminConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<AdminConfig> getAllConfigs() {
        return adminConfigRepository.findAll();
    }

    public Optional<AdminConfig> getConfig(String key) {
        return adminConfigRepository.findByConfigKey(key);
    }

    @Transactional
    public AdminConfig updateConfig(String key, String value, String description) {
        Optional<AdminConfig> existing = adminConfigRepository.findByConfigKey(key);
        if (existing.isPresent()) {
            AdminConfig config = existing.get();
            config.setConfigValue(value);
            config.setDescription(description);
            return adminConfigRepository.save(config);
        }

        AdminConfig config = new AdminConfig(key, value, description);
        return adminConfigRepository.save(config);
    }

    public boolean isTrafficEnabled() {
        return getBoolean("traffic.enabled", false);
    }

    public boolean is404Enabled() {
        return getBoolean("error.404.enabled", false);
    }

    public boolean isSlowSqlEnabled() {
        return getBoolean("sql.slow.enabled", false);
    }

    public boolean isSlowCreditEnabled() {
        return getBoolean("credit.slow.enabled", false);
    }

    public boolean isCpuProblemEnabled() {
        return getBoolean("problem.cpu.enabled", false);
    }

    public int getSlowSqlDelay() {
        return getInt("sql.slow.delay", 0);
    }

    public int getSlowCreditDelay() {
        return getInt("credit.slow.delay", 0);
    }

    public int getCpuProblemMillis() {
        return getInt("problem.cpu.millis", 250);
    }

    public void simulateCpuProblem() {
        if (!isCpuProblemEnabled()) {
            return;
        }

        long deadline = System.nanoTime() + (getCpuProblemMillis() * 1_000_000L);
        double value = 0.0;
        while (System.nanoTime() < deadline) {
            value += Math.sqrt(System.nanoTime() % 997);
        }

        if (value == -1.0) {
            throw new IllegalStateException("Unexpected CPU problem sentinel");
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User createUser(String username, String password, String firstName, String lastName) {
        String accountNumber = generateAccountNumber(username);
        User user = new User(username, password, firstName, lastName, accountNumber);
        user = userRepository.save(user);

        BigDecimal initialBalance = calculateInitialBalance(firstName);
        Account account = new Account(accountNumber);
        account.setBalance(initialBalance);
        accountRepository.save(account);

        return user;
    }

    @Transactional
    public void deleteUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            accountRepository.findByAccountNumber(user.getAccountNumber()).ifPresent(accountRepository::delete);
            userRepository.delete(user);
        }
    }

    @Transactional
    public void createDefaultUsers() {
        createUserIfNotExists("conner.oc", "ConnerC", "Conner", "OC");
        createUserIfNotExists("jack.nible", "JackN", "Jack", "Nible");
        createUserIfNotExists("hunter.done", "HunterD", "Hunter", "Done");
        createUserIfNotExists("henry.esi", "HenryE", "Henry", "Esi");
    }

    private void createUserIfNotExists(String username, String password, String firstName, String lastName) {
        if (userRepository.findByUsername(username).isEmpty()) {
            createUser(username, password, firstName, lastName);
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return getConfig(key).map(c -> "true".equalsIgnoreCase(c.getConfigValue())).orElse(defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        return getConfig(key).map(c -> {
            try {
                return Integer.parseInt(c.getConfigValue());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    private String generateAccountNumber(String username) {
        return String.valueOf(Math.abs(username.hashCode()) % 1000000);
    }

    private BigDecimal calculateInitialBalance(String firstName) {
        return BigDecimal.valueOf(2100L * firstName.length());
    }
}
