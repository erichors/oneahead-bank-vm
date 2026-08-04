package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransferResult;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BankingService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AdminService adminService;
    
    private static final String DEFAULT_ACCOUNT = "1001";
    
    public BigDecimal getBalance() {
        Account account = getOrCreateAccount(DEFAULT_ACCOUNT);
        return account.getBalance();
    }
    
    @Transactional
    public void deposit(BigDecimal amount, String metadata) {
        if (adminService.isSlowSqlEnabled()) {
            try {
                Thread.sleep(adminService.getSlowSqlDelay() * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Account account = getOrCreateAccount(DEFAULT_ACCOUNT);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        Transaction transaction = new Transaction(
            null, DEFAULT_ACCOUNT, amount, Transaction.TransactionType.DEPOSIT, metadata);
        transactionRepository.save(transaction);
    }
    
    @Transactional
    public TransferResult transfer(String toAccount, BigDecimal amount, String metadata) {
        if (adminService.isSlowSqlEnabled()) {
            try {
                Thread.sleep(adminService.getSlowSqlDelay() * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Account fromAccount = getOrCreateAccount(DEFAULT_ACCOUNT);
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        Account toAccountEntity = getOrCreateAccount(toAccount);
        
        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        BigDecimal toBalanceBefore = toAccountEntity.getBalance();
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccountEntity.setBalance(toAccountEntity.getBalance().add(amount));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccountEntity);
        
        Transaction transaction = new Transaction(
            DEFAULT_ACCOUNT, toAccount, amount, Transaction.TransactionType.TRANSFER, metadata);
        transactionRepository.save(transaction);
        
        return new TransferResult(
            transaction.getId(),
            DEFAULT_ACCOUNT,
            toAccount,
            amount,
            fromBalanceBefore,
            fromAccount.getBalance(),
            toBalanceBefore,
            toAccountEntity.getBalance(),
            transaction.getCreatedAt()
        );
    }
    
    private synchronized Account getOrCreateAccount(String accountNumber) {
        Optional<Account> account = accountRepository.findByAccountNumber(accountNumber);
        if (account.isPresent()) {
            return account.get();
        } else {
            Account newAccount = new Account(accountNumber);
            return accountRepository.save(newAccount);
        }
    }
}
