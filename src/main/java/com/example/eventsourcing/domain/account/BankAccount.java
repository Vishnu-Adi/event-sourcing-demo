package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AggregateRoot;
import com.example.eventsourcing.core.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Bank Account aggregate root implementing Event Sourcing.
 * 
 * This class demonstrates the core principles of Event Sourcing:
 * 1. State is derived from events, not stored directly
 * 2. Business operations generate events
 * 3. Events are applied to reconstruct state
 * 4. Business rules are enforced before events are generated
 * 
 * The account maintains its current state by replaying all events,
 * and generates new events when business operations are performed.
 */
public class BankAccount extends AggregateRoot {
    
    // Current state derived from events
    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private boolean isClosed;
    private String closedReason;
    
    /**
     * Constructor for creating a new bank account.
     * 
     * @param accountHolderName The name of the account holder
     * @param accountType The type of account (e.g., "CHECKING", "SAVINGS")
     * @param initialBalance The initial balance
     * @param currency The currency (e.g., "USD", "EUR")
     */
    public BankAccount(String accountHolderName, String accountType, 
                      BigDecimal initialBalance, String currency) {
        super(generateId());
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.balance = initialBalance;
        this.currency = currency;
        this.isClosed = false;
        this.closedReason = null;
        
        // Generate the AccountOpened event
        AccountOpened event = new AccountOpened(
            getId(), 
            1,  // First event has version 1
            1,  // First event has sequence number 1
            accountHolderName, 
            accountType, 
            initialBalance, 
            currency
        );
        
        applyNewEvent(event);
    }
    
    /**
     * Constructor for reconstructing an account from events.
     * 
     * @param accountId The ID of the account
     * @param events The events to replay to reconstruct the state
     */
    public BankAccount(String accountId, List<DomainEvent> events) {
        super(accountId, events);
    }
    
    /**
     * Deposits money into the account.
     * 
     * @param amount The amount to deposit
     * @param description Description of the deposit
     * @param depositedBy Who or what initiated the deposit
     * @return The transaction ID for this deposit
     */
    public String deposit(BigDecimal amount, String description, String depositedBy) {
        if (isClosed) {
            throw new IllegalStateException("Cannot deposit to a closed account");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        String transactionId = generateTransactionId();
        BigDecimal newBalance = balance.add(amount);
        
        MoneyDeposited event = new MoneyDeposited(
            getId(),
            getVersion() + 1,
            getVersion() + 1,
            amount,
            newBalance,
            transactionId,
            description,
            depositedBy
        );
        
        applyNewEvent(event);
        return transactionId;
    }
    
    /**
     * Withdraws money from the account.
     * 
     * @param amount The amount to withdraw
     * @param description Description of the withdrawal
     * @param withdrawnBy Who or what initiated the withdrawal
     * @return The transaction ID for this withdrawal
     * @throws InsufficientFundsException if there are insufficient funds
     */
    public String withdraw(BigDecimal amount, String description, String withdrawnBy) {
        if (isClosed) {
            throw new IllegalStateException("Cannot withdraw from a closed account");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientFundsException(
                getId(), 
                amount.doubleValue(), 
                balance.doubleValue()
            );
        }
        
        String transactionId = generateTransactionId();
        BigDecimal newBalance = balance.subtract(amount);
        
        MoneyWithdrawn event = new MoneyWithdrawn(
            getId(),
            getVersion() + 1,
            getVersion() + 1,
            amount,
            newBalance,
            transactionId,
            description,
            withdrawnBy
        );
        
        applyNewEvent(event);
        return transactionId;
    }
    
    /**
     * Closes the account.
     * 
     * @param reason The reason for closing the account
     * @param closedBy Who or what initiated the closure
     * @param transferAccountId The account where remaining funds should be transferred (optional)
     */
    public void close(String reason, String closedBy, String transferAccountId) {
        if (isClosed) {
            throw new IllegalStateException("Account is already closed");
        }
        
        AccountClosed event = new AccountClosed(
            getId(),
            getVersion() + 1,
            getVersion() + 1,
            balance,
            reason,
            closedBy,
            transferAccountId
        );
        
        applyNewEvent(event);
    }
    
    /**
     * Gets the current balance of the account.
     */
    public BigDecimal getBalance() {
        return balance;
    }
    
    /**
     * Gets the name of the account holder.
     */
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    /**
     * Gets the type of the account.
     */
    public String getAccountType() {
        return accountType;
    }
    
    /**
     * Gets the currency of the account.
     */
    public String getCurrency() {
        return currency;
    }
    
    /**
     * Checks if the account is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }
    
    /**
     * Gets the reason why the account was closed (if closed).
     */
    public String getClosedReason() {
        return closedReason;
    }
    
    /**
     * Handles domain events to update the aggregate's state.
     * 
     * This method is called whenever an event is applied to the aggregate,
     * whether it's a new event or a replayed event. It's responsible for
     * updating the aggregate's state based on the event.
     * 
     * @param event The event to handle
     */
    @Override
    protected void handleEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "AccountOpened":
                handleAccountOpened((AccountOpened) event);
                break;
            case "MoneyDeposited":
                handleMoneyDeposited((MoneyDeposited) event);
                break;
            case "MoneyWithdrawn":
                handleMoneyWithdrawn((MoneyWithdrawn) event);
                break;
            case "AccountClosed":
                handleAccountClosed((AccountClosed) event);
                break;
            default:
                // Ignore unknown events (for forward compatibility)
                break;
        }
    }
    
    /**
     * Handles the AccountOpened event.
     */
    private void handleAccountOpened(AccountOpened event) {
        this.accountHolderName = event.getAccountHolderName();
        this.accountType = event.getAccountType();
        this.balance = event.getInitialBalance();
        this.currency = event.getCurrency();
        this.isClosed = false;
        this.closedReason = null;
    }
    
    /**
     * Handles the MoneyDeposited event.
     */
    private void handleMoneyDeposited(MoneyDeposited event) {
        this.balance = event.getNewBalance();
    }
    
    /**
     * Handles the MoneyWithdrawn event.
     */
    private void handleMoneyWithdrawn(MoneyWithdrawn event) {
        this.balance = event.getNewBalance();
    }
    
    /**
     * Handles the AccountClosed event.
     */
    private void handleAccountClosed(AccountClosed event) {
        this.isClosed = true;
        this.closedReason = event.getReason();
        this.balance = event.getFinalBalance();
    }
    
    /**
     * Generates a new unique identifier.
     */
    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return String.format("BankAccount{id='%s', holder='%s', type='%s', balance=%s %s, closed=%s, version=%d}",
                getId(), accountHolderName, accountType, balance, currency, isClosed, getVersion());
    }
}
