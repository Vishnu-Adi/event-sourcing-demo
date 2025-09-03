package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AggregateRoot;
import com.example.eventsourcing.core.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class BankAccount extends AggregateRoot {
    
    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;
    private boolean isClosed;
    private String closedReason;
    private static final AtomicLong ID_SEQ = new AtomicLong(0);
    
    public BankAccount(String accountHolderName, String accountType,
                      BigDecimal initialBalance) {
        super(generateSequentialId());
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.balance = initialBalance;
        this.isClosed = false;
        this.closedReason = null;
        
        AccountOpened event = new AccountOpened(
            getId(), 
            1,
            1,
            accountHolderName,
            accountType,
            initialBalance
        );
        
        applyNewEvent(event);
    }
    
    public BankAccount(String accountId, List<DomainEvent> events) {
        super(accountId, events);
    }
    
    public String deposit(BigDecimal amount, String description, String depositedBy) {
        if (isClosed) {
            throw new IllegalStateException("Cannot deposit to a closed account");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        return deposit(amount, description, depositedBy, null);
    }
    
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
        return withdraw(amount, description, withdrawnBy, null);
    }

    /**
     * Overload allowing an external transactionId to be provided for correlation (e.g., transfers).
     */
    public String deposit(BigDecimal amount, String description, String depositedBy, String transactionId) {
        if (isClosed) {
            throw new IllegalStateException("Cannot deposit to a closed account");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        String txId = (transactionId == null || transactionId.isEmpty()) ? generateTransactionId() : transactionId;
        BigDecimal newBalance = balance.add(amount);
        MoneyDeposited event = new MoneyDeposited(
            getId(),
            getVersion() + 1,
            getVersion() + 1,
            amount,
            newBalance,
            txId,
            description,
            depositedBy
        );
        applyNewEvent(event);
        return txId;
    }

    /**
     * Overload allowing an external transactionId to be provided for correlation (e.g., transfers).
     */
    public String withdraw(BigDecimal amount, String description, String withdrawnBy, String transactionId) {
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
        String txId = (transactionId == null || transactionId.isEmpty()) ? generateTransactionId() : transactionId;
        BigDecimal newBalance = balance.subtract(amount);
        MoneyWithdrawn event = new MoneyWithdrawn(
            getId(),
            getVersion() + 1,
            getVersion() + 1,
            amount,
            newBalance,
            txId,
            description,
            withdrawnBy
        );
        applyNewEvent(event);
        return txId;
    }
    
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
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    
    
    public boolean isClosed() {
        return isClosed;
    }
    
    public String getClosedReason() {
        return closedReason;
    }
    
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
                break;
        }
    }
    
    private void handleAccountOpened(AccountOpened event) {
        this.accountHolderName = event.getAccountHolderName();
        this.accountType = event.getAccountType();
        this.balance = event.getInitialBalance();
        this.isClosed = false;
        this.closedReason = null;
    }
    
    private void handleMoneyDeposited(MoneyDeposited event) {
        this.balance = event.getNewBalance();
    }
    
    private void handleMoneyWithdrawn(MoneyWithdrawn event) {
        this.balance = event.getNewBalance();
    }
    
    private void handleAccountClosed(AccountClosed event) {
        this.isClosed = true;
        this.closedReason = event.getReason();
        this.balance = event.getFinalBalance();
    }
    
    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    private static String generateSequentialId() {
        return String.valueOf(ID_SEQ.incrementAndGet());
    }
    
    @Override
    public String toString() {
    return String.format("BankAccount{id='%s', holder='%s', type='%s', balance=%s, closed=%s, version=%d}",
        getId(), accountHolderName, accountType, balance, isClosed, getVersion());
    }
}