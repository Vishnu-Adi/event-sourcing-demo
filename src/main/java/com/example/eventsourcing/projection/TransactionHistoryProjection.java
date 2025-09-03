package com.example.eventsourcing.projection;

import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.domain.account.AccountClosed;
import com.example.eventsourcing.domain.account.AccountOpened;
import com.example.eventsourcing.domain.account.MoneyDeposited;
import com.example.eventsourcing.domain.account.MoneyWithdrawn;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TransactionHistoryProjection implements EventProjection {
    
    private final String projectionName;
    private final Map<String, List<TransactionRecord>> transactionsByAccount;
    private final Map<String, List<TransactionRecord>> transactionsById;
    private final List<TransactionRecord> allTransactions;
    
    public static class TransactionRecord {
        private final String transactionId;
        private final String accountId;
        private final String transactionType;
        private final BigDecimal amount;
        private final BigDecimal balanceAfter;
        private final String description;
        private final String performedBy;
        private final Instant occurredAt;
        private final long eventVersion;
        
        public TransactionRecord(String transactionId, String accountId, String transactionType,
                               BigDecimal amount, BigDecimal balanceAfter, String description,
                               String performedBy, Instant occurredAt, long eventVersion) {
            this.transactionId = transactionId;
            this.accountId = accountId;
            this.transactionType = transactionType;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.description = description;
            this.performedBy = performedBy;
            this.occurredAt = occurredAt;
            this.eventVersion = eventVersion;
        }
        
        public String getTransactionId() {
            return transactionId;
        }
        
        public String getAccountId() {
            return accountId;
        }
        
        public String getTransactionType() {
            return transactionType;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getPerformedBy() {
            return performedBy;
        }
        
        public Instant getOccurredAt() {
            return occurredAt;
        }
        
        public long getEventVersion() {
            return eventVersion;
        }
        
        @Override
        public String toString() {
            return String.format("TransactionRecord{id='%s', accountId='%s', type='%s', amount=%s, balanceAfter=%s, description='%s', performedBy='%s', occurredAt=%s}",
                    transactionId, accountId, transactionType, amount, balanceAfter, description, performedBy, occurredAt);
        }
    }
    
    public TransactionHistoryProjection(String projectionName) {
        this.projectionName = projectionName;
        this.transactionsByAccount = new ConcurrentHashMap<>();
    this.transactionsById = new ConcurrentHashMap<>();
        this.allTransactions = Collections.synchronizedList(new ArrayList<>());
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
        TransactionRecord record = new TransactionRecord(
            "ACCOUNT_OPENED_" + event.getAggregateId(),
            event.getAggregateId(),
            "ACCOUNT_OPENED",
            event.getInitialBalance(),
            event.getInitialBalance(),
            "Account opened with initial balance",
            "SYSTEM",
            event.getOccurredAt(),
            event.getAggregateVersion()
        );
        
        addTransaction(record);
    }
    
    private void processMoneyDeposited(MoneyDeposited event) {
        TransactionRecord record = new TransactionRecord(
            event.getTransactionId(),
            event.getAggregateId(),
            "DEPOSIT",
            event.getAmount(),
            event.getNewBalance(),
            event.getDescription(),
            event.getDepositedBy(),
            event.getOccurredAt(),
            event.getAggregateVersion()
        );
        
        addTransaction(record);
    }
    
    private void processMoneyWithdrawn(MoneyWithdrawn event) {
        TransactionRecord record = new TransactionRecord(
            event.getTransactionId(),
            event.getAggregateId(),
            "WITHDRAWAL",
            event.getAmount(),
            event.getNewBalance(),
            event.getDescription(),
            event.getWithdrawnBy(),
            event.getOccurredAt(),
            event.getAggregateVersion()
        );
        
        addTransaction(record);
    }
    
    private void processAccountClosed(AccountClosed event) {
        TransactionRecord record = new TransactionRecord(
            "ACCOUNT_CLOSED_" + event.getAggregateId(),
            event.getAggregateId(),
            "ACCOUNT_CLOSED",
            BigDecimal.ZERO,
            event.getFinalBalance(),
            "Account closed: " + event.getReason(),
            event.getClosedBy(),
            event.getOccurredAt(),
            event.getAggregateVersion()
        );
        
        addTransaction(record);
    }
    
    private void addTransaction(TransactionRecord record) {
        transactionsByAccount.computeIfAbsent(record.getAccountId(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(record);
        
        transactionsById.computeIfAbsent(record.getTransactionId(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(record);
        
        allTransactions.add(record);
    }
    
    @Override
    public void reset() {
        transactionsByAccount.clear();
    transactionsById.clear();
        allTransactions.clear();
    }
    
    @Override
    public Object getState() {
        return new HashMap<>(transactionsByAccount);
    }
    
    public List<TransactionRecord> getTransactionsForAccount(String accountId) {
        List<TransactionRecord> transactions = transactionsByAccount.get(accountId);
        if (transactions == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(transactions);
    }
    
    public List<TransactionRecord> getTransactionsForAccount(String accountId, Instant fromDate, Instant toDate) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> !transaction.getOccurredAt().isBefore(fromDate) && 
                                 !transaction.getOccurredAt().isAfter(toDate))
            .collect(Collectors.toList());
    }
    
    public TransactionRecord getTransactionById(String transactionId) {
        List<TransactionRecord> list = transactionsById.get(transactionId);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public List<TransactionRecord> getTransactionsById(String transactionId) {
        List<TransactionRecord> list = transactionsById.get(transactionId);
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }
    
    public List<TransactionRecord> getTransactionsByType(String transactionType) {
        return allTransactions.stream()
            .filter(transaction -> transactionType.equals(transaction.getTransactionType()))
            .collect(Collectors.toList());
    }
    
    public List<TransactionRecord> getTransactionsInDateRange(Instant fromDate, Instant toDate) {
        return allTransactions.stream()
            .filter(transaction -> !transaction.getOccurredAt().isBefore(fromDate) && 
                                 !transaction.getOccurredAt().isAfter(toDate))
            .collect(Collectors.toList());
    }
    
    public BigDecimal getTotalDepositsForAccount(String accountId) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> "DEPOSIT".equals(transaction.getTransactionType()))
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalWithdrawalsForAccount(String accountId) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> "WITHDRAWAL".equals(transaction.getTransactionType()))
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public long getTransactionCountForAccount(String accountId) {
        List<TransactionRecord> transactions = transactionsByAccount.get(accountId);
        return transactions != null ? transactions.size() : 0;
    }
    
    public long getTotalTransactionCount() {
        return allTransactions.size();
    }
    
    @Override
    public boolean isValid() {
        return allTransactions.stream()
            .allMatch(transaction -> transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0);
    }
    
    @Override
    public String toString() {
        return String.format("TransactionHistoryProjection{name='%s', totalTransactions=%d, accountsWithTransactions=%d}",
                projectionName, getTotalTransactionCount(), transactionsByAccount.size());
    }
}