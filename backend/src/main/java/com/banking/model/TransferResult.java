package com.banking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResult {
    private Long transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private BigDecimal fromBalanceBefore;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceBefore;
    private BigDecimal toBalanceAfter;
    private LocalDateTime timestamp;
    
    public TransferResult() {}
    
    public TransferResult(Long transactionId, String fromAccount, String toAccount, 
                         BigDecimal amount, BigDecimal fromBalanceBefore, BigDecimal fromBalanceAfter,
                         BigDecimal toBalanceBefore, BigDecimal toBalanceAfter, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.fromBalanceBefore = fromBalanceBefore;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceBefore = toBalanceBefore;
        this.toBalanceAfter = toBalanceAfter;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getFromAccount() {
        return fromAccount;
    }
    
    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }
    
    public String getToAccount() {
        return toAccount;
    }
    
    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getFromBalanceBefore() {
        return fromBalanceBefore;
    }
    
    public void setFromBalanceBefore(BigDecimal fromBalanceBefore) {
        this.fromBalanceBefore = fromBalanceBefore;
    }
    
    public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter;
    }
    
    public void setFromBalanceAfter(BigDecimal fromBalanceAfter) {
        this.fromBalanceAfter = fromBalanceAfter;
    }
    
    public BigDecimal getToBalanceBefore() {
        return toBalanceBefore;
    }
    
    public void setToBalanceBefore(BigDecimal toBalanceBefore) {
        this.toBalanceBefore = toBalanceBefore;
    }
    
    public BigDecimal getToBalanceAfter() {
        return toBalanceAfter;
    }
    
    public void setToBalanceAfter(BigDecimal toBalanceAfter) {
        this.toBalanceAfter = toBalanceAfter;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
