package com.banking.controller;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private static final String[] DEBIT_MERCHANTS = {
        "Harbor Table Dinner",
        "Summit Auto Payment",
        "Oakstone Mortgage",
        "Northline Groceries",
        "Bright Pump Gas",
        "Cafe Meridian Lunch",
        "Happy Paws Pet Store",
        "Metro Utilities",
        "Cobalt Market",
        "Riverbend Bistro",
        "Lakefront Pharmacy",
        "Bluebird Home Supply"
    };

    private static final String[] CREDIT_SOURCES = {
        "Direct deposit",
        "Statement credit",
        "Expense reimbursement",
        "OneAhead rewards",
        "Mobile check deposit"
    };

    private final Random random = new Random();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> users() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::demoUser).toList());
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        String username = String.valueOf(request.get("username"));
        String password = String.valueOf(request.get("password"));

        return userRepository.findByUsername(username)
            .filter(user -> user.getPassword().equals(password))
            .map(user -> ResponseEntity.ok(accountSummary(user)))
            .orElseGet(() -> {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Invalid demo login");
                return ResponseEntity.badRequest().body(error);
            });
    }

    @GetMapping("/account/{username}")
    public ResponseEntity<Map<String, Object>> account(@PathVariable String username) {
        return userRepository.findByUsername(username)
            .map(user -> ResponseEntity.ok(accountSummary(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transactions/random")
    public ResponseEntity<Map<String, Object>> randomTransaction() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No demo users available"));
        }

        User user = users.get(random.nextInt(users.size()));
        Account account = accountRepository.findByAccountNumber(user.getAccountNumber())
            .orElseGet(() -> accountRepository.save(new Account(user.getAccountNumber())));
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        boolean credit = random.nextInt(10) < 3;
        BigDecimal amount = randomAmount(credit);
        String description = credit
            ? CREDIT_SOURCES[random.nextInt(CREDIT_SOURCES.length)]
            : DEBIT_MERCHANTS[random.nextInt(DEBIT_MERCHANTS.length)];

        Transaction transaction;
        if (credit) {
            account.setBalance(account.getBalance().add(amount));
            transaction = new Transaction(null, account.getAccountNumber(), amount, Transaction.TransactionType.DEPOSIT, description);
        } else {
            account.setBalance(account.getBalance().subtract(amount));
            transaction = new Transaction(account.getAccountNumber(), merchantAccount(description), amount, Transaction.TransactionType.TRANSFER, description);
        }

        accountRepository.save(account);
        transactionRepository.save(transaction);

        Map<String, Object> response = transaction(transaction);
        response.put("username", user.getUsername());
        response.put("name", user.getFirstName() + " " + user.getLastName());
        response.put("balance", account.getBalance());
        response.put("direction", credit ? "credit" : "debit");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> accountSummary(User user) {
        Account account = accountRepository.findByAccountNumber(user.getAccountNumber())
            .orElseGet(() -> {
                Account next = new Account(user.getAccountNumber());
                return accountRepository.save(next);
            });

        Map<String, Object> response = new HashMap<>();
        response.put("user", demoUser(user));
        response.put("account", Map.of(
            "type", "Checking",
            "accountNumber", account.getAccountNumber(),
            "lastFour", lastFour(account.getAccountNumber()),
            "balance", account.getBalance() == null ? BigDecimal.ZERO : account.getBalance()
        ));
        response.put("transactions", transactionRepository
            .findByFromAccountOrToAccountOrderByCreatedAtDesc(account.getAccountNumber(), account.getAccountNumber())
            .stream()
            .map(this::transaction)
            .toList());
        return response;
    }

    private Map<String, Object> demoUser(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("password", user.getPassword());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("name", user.getFirstName() + " " + user.getLastName());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("address", user.getAddress());
        response.put("accountNumber", user.getAccountNumber());
        response.put("lastFour", lastFour(user.getAccountNumber()));
        return response;
    }

    private Map<String, Object> transaction(Transaction transaction) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", transaction.getId());
        response.put("type", transaction.getType());
        response.put("amount", transaction.getAmount());
        response.put("description", transaction.getMetadata());
        response.put("fromAccount", transaction.getFromAccount());
        response.put("toAccount", transaction.getToAccount());
        response.put("createdAt", transaction.getCreatedAt());
        return response;
    }

    private String lastFour(String value) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        return value.substring(value.length() - 4);
    }

    private BigDecimal randomAmount(boolean credit) {
        int cents = credit
            ? random.nextInt(240000) + 2500
            : random.nextInt(22000) + 650;
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private String merchantAccount(String description) {
        return String.valueOf(Math.abs(description.hashCode()) % 900000 + 100000);
    }
}
