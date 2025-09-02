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

/**
 * Projection that maintains a complete transaction history for all accounts.
 * 
 * This projection demonstrates how to build a more complex read model from events.
 * It tracks all transactions (deposits, withdrawals, account openings, closures)
 * and provides various query capabilities for transaction history.
 * 
 * Use cases:
 * - Transaction history reports
 * - Audit trails
 * - Transaction analysis and reporting
 * - Compliance and regulatory reporting
 * - Customer transaction statements
 */
public class TransactionHistoryProjection implements EventProjection {
    
    private final String projectionName;
    private final Map<String, List<TransactionRecord>> transactionsByAccount;
    private final Map<String, TransactionRecord> transactionsById;
    private final List<TransactionRecord> allTransactions;
    
    /**
     * Internal class to represent a transaction record.
     */
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
    
    /**
     * Constructor for creating a new transaction history projection.
     * 
     * @param projectionName The name of this projection
     */
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
                // Ignore unknown events
                break;
        }
    }
    
    /**
     * Processes an AccountOpened event.
     */
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
    
    /**
     * Processes a MoneyDeposited event.
     */
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
    
    /**
     * Processes a MoneyWithdrawn event.
     */
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
    
    /**
     * Processes an AccountClosed event.
     */
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
    
    /**
     * Adds a transaction record to the projection.
     */
    private void addTransaction(TransactionRecord record) {
        // Add to account-specific list
        transactionsByAccount.computeIfAbsent(record.getAccountId(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(record);
        
        // Add to global transaction map
        transactionsById.put(record.getTransactionId(), record);
        
        // Add to global list
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
    
    /**
     * Gets all transactions for a specific account.
     * 
     * @param accountId The ID of the account
     * @return A list of transactions for the account, ordered by occurrence time
     */
    public List<TransactionRecord> getTransactionsForAccount(String accountId) {
        List<TransactionRecord> transactions = transactionsByAccount.get(accountId);
        if (transactions == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(transactions);
    }
    
    /**
     * Gets transactions for a specific account within a date range.
     * 
     * @param accountId The ID of the account
     * @param fromDate The start date (inclusive)
     * @param toDate The end date (inclusive)
     * @return A list of transactions within the date range
     */
    public List<TransactionRecord> getTransactionsForAccount(String accountId, Instant fromDate, Instant toDate) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> !transaction.getOccurredAt().isBefore(fromDate) && 
                                 !transaction.getOccurredAt().isAfter(toDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets a specific transaction by its ID.
     * 
     * @param transactionId The ID of the transaction
     * @return The transaction record, or null if not found
     */
    public TransactionRecord getTransactionById(String transactionId) {
        return transactionsById.get(transactionId);
    }
    
    /**
     * Gets all transactions of a specific type.
     * 
     * @param transactionType The type of transaction (e.g., "DEPOSIT", "WITHDRAWAL")
     * @return A list of transactions of the specified type
     */
    public List<TransactionRecord> getTransactionsByType(String transactionType) {
        return allTransactions.stream()
            .filter(transaction -> transactionType.equals(transaction.getTransactionType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all transactions within a date range.
     * 
     * @param fromDate The start date (inclusive)
     * @param toDate The end date (inclusive)
     * @return A list of transactions within the date range
     */
    public List<TransactionRecord> getTransactionsInDateRange(Instant fromDate, Instant toDate) {
        return allTransactions.stream()
            .filter(transaction -> !transaction.getOccurredAt().isBefore(fromDate) && 
                                 !transaction.getOccurredAt().isAfter(toDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the total amount of deposits for a specific account.
     * 
     * @param accountId The ID of the account
     * @return The total amount of deposits
     */
    public BigDecimal getTotalDepositsForAccount(String accountId) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> "DEPOSIT".equals(transaction.getTransactionType()))
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the total amount of withdrawals for a specific account.
     * 
     * @param accountId The ID of the account
     * @return The total amount of withdrawals
     */
    public BigDecimal getTotalWithdrawalsForAccount(String accountId) {
        return getTransactionsForAccount(accountId).stream()
            .filter(transaction -> "WITHDRAWAL".equals(transaction.getTransactionType()))
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the total number of transactions for a specific account.
     * 
     * @param accountId The ID of the account
     * @return The total number of transactions
     */
    public long getTransactionCountForAccount(String accountId) {
        List<TransactionRecord> transactions = transactionsByAccount.get(accountId);
        return transactions != null ? transactions.size() : 0;
    }
    
    /**
     * Gets the total number of transactions across all accounts.
     * 
     * @return The total number of transactions
     */
    public long getTotalTransactionCount() {
        return allTransactions.size();
    }
    
    @Override
    public boolean isValid() {
        // Basic validation: all transactions should have valid amounts
        return allTransactions.stream()
            .allMatch(transaction -> transaction.getAmount().compareTo(BigDecimal.ZERO) >= 0);
    }
    
    @Override
    public String toString() {
        return String.format("TransactionHistoryProjection{name='%s', totalTransactions=%d, accountsWithTransactions=%d}",
                projectionName, getTotalTransactionCount(), transactionsByAccount.size());
    }
}
