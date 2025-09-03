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

public class AccountBalanceProjection implements EventProjection {
    
    private final String projectionName;
    private final Map<String, AccountBalance> accountBalances;
    
    public static class AccountBalance {
        private final String accountId;
        private final String accountHolderName;
        private final String accountType;
        private final BigDecimal balance;
        
        private final boolean isClosed;
        private final long lastEventVersion;
        
        public AccountBalance(String accountId, String accountHolderName, String accountType,
                            BigDecimal balance, boolean isClosed, long lastEventVersion) {
            this.accountId = accountId;
            this.accountHolderName = accountHolderName;
            this.accountType = accountType;
            this.balance = balance;
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
        
        
        public boolean isClosed() {
            return isClosed;
        }
        
        public long getLastEventVersion() {
            return lastEventVersion;
        }
        
        @Override
        public String toString() {
        return String.format("AccountBalance{id='%s', holder='%s', type='%s', balance=%s, closed=%s, version=%d}",
            accountId, accountHolderName, accountType, balance, isClosed, lastEventVersion);
        }
    }
    
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
                break;
        }
    }
    
    private void processAccountOpened(AccountOpened event) {
        AccountBalance balance = new AccountBalance(
            event.getAggregateId(),
            event.getAccountHolderName(),
            event.getAccountType(),
            event.getInitialBalance(),
            false,
            event.getAggregateVersion()
        );
        
        accountBalances.put(event.getAggregateId(), balance);
    }
    
    private void processMoneyDeposited(MoneyDeposited event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getNewBalance(),
                currentBalance.isClosed(),
                event.getAggregateVersion()
            );
            
            accountBalances.put(event.getAggregateId(), updatedBalance);
        }
    }
    
    private void processMoneyWithdrawn(MoneyWithdrawn event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getNewBalance(),
                currentBalance.isClosed(),
                event.getAggregateVersion()
            );
            
            accountBalances.put(event.getAggregateId(), updatedBalance);
        }
    }
    
    private void processAccountClosed(AccountClosed event) {
        AccountBalance currentBalance = accountBalances.get(event.getAggregateId());
        if (currentBalance != null) {
            AccountBalance updatedBalance = new AccountBalance(
                currentBalance.getAccountId(),
                currentBalance.getAccountHolderName(),
                currentBalance.getAccountType(),
                event.getFinalBalance(),
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
    
    public AccountBalance getAccountBalance(String accountId) {
        return accountBalances.get(accountId);
    }
    
    public Map<String, AccountBalance> getAllAccountBalances() {
        return new HashMap<>(accountBalances);
    }
    
    public BigDecimal getTotalBalanceByType(String accountType) {
        return accountBalances.values().stream()
            .filter(balance -> accountType.equals(balance.getAccountType()) && !balance.isClosed())
            .map(AccountBalance::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalBalance() {
        return accountBalances.values().stream()
            .filter(balance -> !balance.isClosed())
            .map(AccountBalance::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
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
        return accountBalances.values().stream()
            .allMatch(balance -> balance.getBalance().compareTo(BigDecimal.ZERO) >= 0);
    }
    
    @Override
    public String toString() {
        return String.format("AccountBalanceProjection{name='%s', accountCount=%d, totalBalance=%s}",
                projectionName, accountBalances.size(), getTotalBalance());
    }
}