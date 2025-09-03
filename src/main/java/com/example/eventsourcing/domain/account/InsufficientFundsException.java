package com.example.eventsourcing.domain.account;

public class InsufficientFundsException extends RuntimeException {
    
    private final String accountId;
    private final double requestedAmount;
    private final double availableBalance;
    
    public InsufficientFundsException(String accountId, double requestedAmount, double availableBalance) {
        super(String.format("Insufficient funds in account %s: requested %.2f, but only %.2f available",
                accountId, requestedAmount, availableBalance));
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public double getRequestedAmount() {
        return requestedAmount;
    }
    
    public double getAvailableBalance() {
        return availableBalance;
    }
}