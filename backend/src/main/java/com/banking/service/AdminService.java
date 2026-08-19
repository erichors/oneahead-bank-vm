package com.banking.service;

import com.banking.model.Account;
import com.banking.model.AdminConfig;
import com.banking.model.Transaction;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.AdminConfigRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Autowired
    private TransactionRepository transactionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDemoData() {
        createDefaultUsers();
        userRepository.findAll().forEach(this::seedTransactionsForUser);
    }

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
    public User createUser(String username, String password, String firstName, String lastName, String address, String city, String state, BigDecimal initialBalance) {
        String accountNumber = generateAccountNumber(username);
        User user = new User(username, password, firstName, lastName, address, city, state, accountNumber);
        user = userRepository.save(user);

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
        createUserIfNotExists("dmorgan", "ahead1", "Dave", "Morgan", "2148 Aurora Avenue", "Naperville", "Illinois", BigDecimal.valueOf(18420.63));
        createUserIfNotExists("tbrady", "goat", "Thomas", "Brady", "82 Biscayne Terrace", "Miami", "Florida", BigDecimal.valueOf(128704.12));
        createUserIfNotExists("mlowe", "ahead1", "Matt", "Lowe", "4406 Clifton Boulevard", "Cleveland", "Ohio", BigDecimal.valueOf(9204.44));
        createUserIfNotExists("dshah", "ahead1", "Dipen", "Shah", "17 Oak Tree Road", "Edison", "New Jersey", BigDecimal.valueOf(35672.88));
    }

    private void createUserIfNotExists(String username, String password, String firstName, String lastName) {
        if (userRepository.findByUsername(username).isEmpty()) {
            createUser(username, password, firstName, lastName);
        }
    }

    private void createUserIfNotExists(String username, String password, String firstName, String lastName, String address, String city, String state, BigDecimal initialBalance) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            createUser(username, password, firstName, lastName, address, city, state, initialBalance);
        } else {
            User user = existing.get();
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setAddress(address);
            user.setCity(city);
            user.setState(state);
            userRepository.save(user);
        }
    }

    private void seedTransactionsForUser(User user) {
        String accountNumber = user.getAccountNumber();
        if (transactionRepository.existsByFromAccountOrToAccount(accountNumber, accountNumber)) {
            return;
        }

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(null, accountNumber, BigDecimal.valueOf(4825.00), Transaction.TransactionType.DEPOSIT, "Direct deposit"));
        transactions.add(new Transaction(accountNumber, "883421", BigDecimal.valueOf(124.42), Transaction.TransactionType.TRANSFER, "Blue Harbor Dinner"));
        transactions.add(new Transaction(accountNumber, "772910", BigDecimal.valueOf(86.19), Transaction.TransactionType.TRANSFER, "Northline Groceries"));
        transactions.add(new Transaction(null, accountNumber, BigDecimal.valueOf(72.35), Transaction.TransactionType.DEPOSIT, "Statement credit"));
        transactions.add(new Transaction(accountNumber, "551208", BigDecimal.valueOf(240.00), Transaction.TransactionType.TRANSFER, "Metro Utilities"));
        transactionRepository.saveAll(transactions);
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
