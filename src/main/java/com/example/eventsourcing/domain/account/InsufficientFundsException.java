package com.example.eventsourcing.domain.account;

/**
 * Exception thrown when attempting to withdraw more money than is available in an account.
 * 
 * This is a domain-specific exception that represents a business rule violation.
 * In Event Sourcing, business rules are enforced in the aggregate root before
 * events are generated, ensuring that invalid operations don't create events.
 */
public class InsufficientFundsException extends RuntimeException {
    
    private final String accountId;
    private final double requestedAmount;
    private final double availableBalance;
    
    /**
     * Creates a new InsufficientFundsException.
     * 
     * @param accountId The ID of the account that has insufficient funds
     * @param requestedAmount The amount that was requested for withdrawal
     * @param availableBalance The current available balance in the account
     */
    public InsufficientFundsException(String accountId, double requestedAmount, double availableBalance) {
        super(String.format("Insufficient funds in account %s: requested %.2f, but only %.2f available",
                accountId, requestedAmount, availableBalance));
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }
    
    /**
     * Gets the ID of the account that has insufficient funds.
     */
    public String getAccountId() {
        return accountId;
    }
    
    /**
     * Gets the amount that was requested for withdrawal.
     */
    public double getRequestedAmount() {
        return requestedAmount;
    }
    
    /**
     * Gets the current available balance in the account.
     */
    public double getAvailableBalance() {
        return availableBalance;
    }
}
