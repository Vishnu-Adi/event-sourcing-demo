package com.example.eventsourcing.projection;

import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.domain.account.AccountClosed;
import com.example.eventsourcing.domain.account.AccountOpened;
import com.example.eventsourcing.domain.account.MoneyDeposited;
import com.example.eventsourcing.domain.account.MoneyWithdrawn;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Projection that maintains the current balance for all accounts.
 * 
 * This projection demonstrates how to build a simple read model from events.
 * It tracks the current balance of each account by processing account-related
 * events in the order they occurred.
 * 
 * Use cases:
 * - Quick balance lookups without replaying all events
 * - Account balance reports
 * - Balance validation for business operations
 * - Real-time balance updates for user interfaces
 */
public class AccountBalanceProjection implements EventProjection {
    
    private final String projectionName;
    private final Map<String, AccountBalance> accountBalances;
    
    /**
     * Internal class to hold account balance information.
     */
    public static class AccountBalance {
        private final String accountId;
        private final String accountHolderName;
        private final String accountType;
        private final BigDecimal balance;
        private final String currency;
        private final boolean isClosed;
        private final long lastEventVersion;
        
        public AccountBalance(String accountId, String accountHolderName, String accountType,
                            BigDecimal balance, String currency, boolean isClosed, long lastEventVersion) {
            this.accountId = accountId;
            this.accountHolderName = accountHolderName;
            this.accountType = accountType;
            this.balance = balance;
            this.currency = currency;
            this.isClosed = isClosed;
            this.lastEventVersion = lastEventVersion;
        }
        
        public String getAccountId() {
            return accountId;
        }
        
        public String getAccountHolderName() {
            return accountHolderName;
        }
        
        public String getAccountType() {
            return accountType;
        }
        
        public BigDecimal getBalance() {
            return balance;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public boolean isClosed() {
            return isClosed;
        }
        
        public long getLastEventVersion() {
            return lastEventVersion;
        }
        
        @Override
        public String toString() {
            return String.format("AccountBalance{id='%s', holder='%s', type='%s', balance=%s %s, closed=%s, version=%d}",
                    accountId, accountHolderName, accountType, balance, currency, isClosed, lastEventVersion);
        }
    }
    
    /**
     * Constructor for creating a new account balance projection.
     * 
     * @param projectionName The name of this projection
     */
    public AccountBalanceProjection(String projectionName) {
        this.projectionName = projectionName;
        this.accountBalances = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getProjectionName() {
        return projectionName;
    }
    
    @Override
    public void processEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "AccountOpened":
                processAccountOpened((AccountOpened) event);
                break;
            case "MoneyDeposited":
                processMoneyDeposited((MoneyDeposited) event);
                break;
            case "MoneyWithdrawn":
                processMoneyWithdrawn((MoneyWithdrawn) event);
                break;
            case "AccountClosed":
                processAccountClosed((AccountClosed) event);
                break;
            default:
                // Ignore unknown events
                break;
        }
    }
    
    /**
     * Processes an AccountOpened event.
     */
    private void processAccountOpened(AccountOpened event) {
        AccountBalance balance = new AccountBalance(
            event.getAggregateId(),
            event.getAccountHolderName(),
            event.getAccountType(),
            event.getInitialBalance(),
            event.getCurrency(),
            false,
            event.getAggregateVersion()
        );
        
        accountBalances.put(event.getAggregateId(), balance);
    }
    
    /**
     * Processes a MoneyDeposited event.
     */
    private void processMoneyDeposited(MoneyDeposited event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getNewBalance(),
                currentBalance.getCurrency(),
                currentBalance.isClosed(),
                event.getAggregateVersion()
            );
            
            accountBalances.put(event.getAggregateId(), updatedBalance);
        }
    }
    
    /**
     * Processes a MoneyWithdrawn event.
     */
    private void processMoneyWithdrawn(MoneyWithdrawn event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getNewBalance(),
                currentBalance.getCurrency(),
                currentBalance.isClosed(),
                event.getAggregateVersion()
            );
            
            accountBalances.put(event.getAggregateId(), updatedBalance);
        }
    }
    
    /**
     * Processes an AccountClosed event.
     */
    private void processAccountClosed(AccountClosed event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getFinalBalance(),
                currentBalance.getCurrency(),
                true,
                event.getAggregateVersion()
            );
            
            accountBalances.put(event.getAggregateId(), updatedBalance);
        }
    }
    
    @Override
    public void reset() {
        accountBalances.clear();
    }
    
    @Override
    public Object getState() {
        return new HashMap<>(accountBalances);
    }
    
    /**
     * Gets the current balance for a specific account.
     * 
     * @param accountId The ID of the account
     * @return The current balance, or null if the account doesn't exist
     */
    public AccountBalance getAccountBalance(String accountId) {
        return accountBalances.get(accountId);
    }
    
    /**
     * Gets all account balances.
     * 
     * @return A map of account ID to account balance
     */
    public Map<String, AccountBalance> getAllAccountBalances() {
        return new HashMap<>(accountBalances);
    }
    
    /**
     * Gets the total balance across all accounts of a specific type.
     * 
     * @param accountType The type of account to sum
     * @return The total balance for the specified account type
     */
    public BigDecimal getTotalBalanceByType(String accountType) {
        return accountBalances.values().stream()
            .filter(balance -> accountType.equals(balance.getAccountType()) && !balance.isClosed())
            .map(AccountBalance::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the total balance across all accounts.
     * 
     * @return The total balance across all open accounts
     */
    public BigDecimal getTotalBalance() {
        return accountBalances.values().stream()
            .filter(balance -> !balance.isClosed())
            .map(AccountBalance::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the number of accounts by type.
     * 
     * @return A map of account type to count
     */
    public Map<String, Long> getAccountCountByType() {
        Map<String, Long> counts = new HashMap<>();
        for (AccountBalance balance : accountBalances.values()) {
            if (!balance.isClosed()) {
                counts.merge(balance.getAccountType(), 1L, Long::sum);
            }
        }
        return counts;
    }
    
    @Override
    public boolean isValid() {
        // Basic validation: all balances should be non-negative
        return accountBalances.values().stream()
            .allMatch(balance -> balance.getBalance().compareTo(BigDecimal.ZERO) >= 0);
    }
    
    @Override
    public String toString() {
        return String.format("AccountBalanceProjection{name='%s', accountCount=%d, totalBalance=%s}",
                projectionName, accountBalances.size(), getTotalBalance());
    }
}
