package com.example.eventsourcing.demo;

import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.core.EventStore;
import com.example.eventsourcing.domain.account.BankAccount;
import com.example.eventsourcing.domain.account.InsufficientFundsException;
import com.example.eventsourcing.projection.AccountBalanceProjection;
import com.example.eventsourcing.projection.TransactionHistoryProjection;
import com.example.eventsourcing.store.InMemoryEventStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive demonstration of Event Sourcing principles and implementation.
 * 
 * This demo showcases:
 * 1. Creating and managing bank accounts using Event Sourcing
 * 2. Performing transactions (deposits and withdrawals)
 * 3. Building and querying projections
 * 4. Event replay and state reconstruction
 * 5. Concurrency control and optimistic locking
 * 6. Event store statistics and monitoring
 * 
 * The demo is designed to be educational and shows the key benefits
 * of Event Sourcing: audit trails, temporal queries, and flexible read models.
 */
public class EventSourcingDemo {
    
    private final EventStore eventStore;
    private final AccountBalanceProjection balanceProjection;
    private final TransactionHistoryProjection transactionProjection;
    
    /**
     * Constructor for creating a new Event Sourcing demo.
     */
    public EventSourcingDemo() {
        this.eventStore = new InMemoryEventStore();
        this.balanceProjection = new AccountBalanceProjection("AccountBalanceProjection");
        this.transactionProjection = new TransactionHistoryProjection("TransactionHistoryProjection");
    }
    
    /**
     * Runs the complete Event Sourcing demonstration.
     */
    public void runDemo() {
        System.out.println("=== Event Sourcing Demo ===\n");
        
        try {
            // Step 1: Create accounts
            System.out.println("Step 1: Creating Bank Accounts");
            System.out.println("==============================");
            createAccounts();
            
            // Step 2: Perform transactions
            System.out.println("\nStep 2: Performing Transactions");
            System.out.println("===============================");
            performTransactions();
            
            // Step 3: Demonstrate projections
            System.out.println("\nStep 3: Building and Querying Projections");
            System.out.println("=========================================");
            demonstrateProjections();
            
            // Step 4: Show event replay
            System.out.println("\nStep 4: Event Replay and State Reconstruction");
            System.out.println("=============================================");
            demonstrateEventReplay();
            
            // Step 5: Show concurrency control
            System.out.println("\nStep 5: Concurrency Control");
            System.out.println("===========================");
            demonstrateConcurrencyControl();
            
            // Step 6: Show event store statistics
            System.out.println("\nStep 6: Event Store Statistics");
            System.out.println("==============================");
            showEventStoreStatistics();
            
            // Step 7: Show temporal queries
            System.out.println("\nStep 7: Temporal Queries");
            System.out.println("========================");
            demonstrateTemporalQueries();
            
        } catch (Exception e) {
            System.err.println("Demo failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates several bank accounts to demonstrate account creation.
     */
    private void createAccounts() throws Exception {
        // Create account for John Doe
        BankAccount johnAccount = new BankAccount(
            "John Doe", 
            "CHECKING", 
            new BigDecimal("1000.00"), 
            "USD"
        );
        
        // Save the account to the event store
        saveAccount(johnAccount);
        System.out.println("Created account for John Doe: " + johnAccount);
        
        // Create account for Jane Smith
        BankAccount janeAccount = new BankAccount(
            "Jane Smith", 
            "SAVINGS", 
            new BigDecimal("5000.00"), 
            "USD"
        );
        
        saveAccount(janeAccount);
        System.out.println("Created account for Jane Smith: " + janeAccount);
        
        // Create account for Bob Johnson
        BankAccount bobAccount = new BankAccount(
            "Bob Johnson", 
            "CHECKING", 
            new BigDecimal("2500.00"), 
            "USD"
        );
        
        saveAccount(bobAccount);
        System.out.println("Created account for Bob Johnson: " + bobAccount);
        
        // Update projections with the new events
        updateProjections();
    }
    
    /**
     * Performs various transactions to demonstrate event generation.
     */
    private void performTransactions() throws Exception {
        // Get all accounts
        List<BankAccount> accounts = loadAllAccounts();
        
        if (accounts.isEmpty()) {
            System.out.println("No accounts found. Cannot perform transactions.");
            return;
        }
        
        BankAccount johnAccount = accounts.get(0);
        BankAccount janeAccount = accounts.get(1);
        BankAccount bobAccount = accounts.get(2);
        
        // John deposits money
        String depositId1 = johnAccount.deposit(
            new BigDecimal("500.00"), 
            "Salary deposit", 
            "John Doe"
        );
        saveAccount(johnAccount);
        System.out.println("John deposited $500.00 (Transaction ID: " + depositId1 + ")");
        
        // Jane withdraws money
        String withdrawalId1 = janeAccount.withdraw(
            new BigDecimal("1000.00"), 
            "Vacation fund", 
            "Jane Smith"
        );
        saveAccount(janeAccount);
        System.out.println("Jane withdrew $1000.00 (Transaction ID: " + withdrawalId1 + ")");
        
        // Bob transfers money (deposit to John, withdraw from Bob)
        String withdrawalId2 = bobAccount.withdraw(
            new BigDecimal("200.00"), 
            "Transfer to John", 
            "Bob Johnson"
        );
        saveAccount(bobAccount);
        
        String depositId2 = johnAccount.deposit(
            new BigDecimal("200.00"), 
            "Transfer from Bob", 
            "Bob Johnson"
        );
        saveAccount(johnAccount);
        System.out.println("Bob transferred $200.00 to John");
        
        // Try to withdraw more than available (should fail)
        try {
            johnAccount.withdraw(
                new BigDecimal("2000.00"), 
                "Large withdrawal", 
                "John Doe"
            );
            saveAccount(johnAccount);
        } catch (InsufficientFundsException e) {
            System.out.println("Failed to withdraw $2000.00 from John's account: " + e.getMessage());
        }
        
        // Update projections
        updateProjections();
    }
    
    /**
     * Demonstrates how projections work and how to query them.
     */
    private void demonstrateProjections() throws Exception {
        System.out.println("Account Balance Projection:");
        System.out.println("--------------------------");
        
        // Show all account balances
        balanceProjection.getAllAccountBalances().forEach((accountId, balance) -> {
            System.out.println("  " + balance);
        });
        
        System.out.println("\nTotal balance across all accounts: $" + balanceProjection.getTotalBalance());
        System.out.println("Total checking accounts balance: $" + balanceProjection.getTotalBalanceByType("CHECKING"));
        System.out.println("Total savings accounts balance: $" + balanceProjection.getTotalBalanceByType("SAVINGS"));
        
        System.out.println("\nAccount count by type:");
        balanceProjection.getAccountCountByType().forEach((type, count) -> {
            System.out.println("  " + type + ": " + count + " accounts");
        });
        
        System.out.println("\nTransaction History Projection:");
        System.out.println("-------------------------------");
        
        // Show transaction counts
        System.out.println("Total transactions: " + transactionProjection.getTotalTransactionCount());
        
        // Show transactions by type
        System.out.println("Deposits: " + transactionProjection.getTransactionsByType("DEPOSIT").size());
        System.out.println("Withdrawals: " + transactionProjection.getTransactionsByType("WITHDRAWAL").size());
        System.out.println("Account openings: " + transactionProjection.getTransactionsByType("ACCOUNT_OPENED").size());
        
        // Show recent transactions for first account
        List<BankAccount> accounts = loadAllAccounts();
        if (!accounts.isEmpty()) {
            String accountId = accounts.get(0).getId();
            List<TransactionHistoryProjection.TransactionRecord> transactions = 
                transactionProjection.getTransactionsForAccount(accountId);
            
            System.out.println("\nRecent transactions for " + accountId + ":");
            transactions.forEach(transaction -> {
                System.out.println("  " + transaction);
            });
        }
    }
    
    /**
     * Demonstrates event replay and state reconstruction.
     */
    private void demonstrateEventReplay() throws Exception {
        System.out.println("Demonstrating Event Replay:");
        System.out.println("---------------------------");
        
        // Get all events from the event store
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        System.out.println("Total events in store: " + allEvents.size());
        
        // Show events in chronological order
        System.out.println("\nEvents in chronological order:");
        allEvents.forEach(event -> {
            System.out.println("  " + event);
        });
        
        // Reconstruct an account from events
        List<BankAccount> accounts = loadAllAccounts();
        if (!accounts.isEmpty()) {
            String accountId = accounts.get(0).getId();
            List<DomainEvent> accountEvents = eventStore.getEvents(accountId).get();
            
            System.out.println("\nReconstructing account " + accountId + " from events:");
            System.out.println("Events for this account: " + accountEvents.size());
            
            // Create a new account instance by replaying events
            BankAccount reconstructedAccount = new BankAccount(accountId, accountEvents);
            System.out.println("Reconstructed account: " + reconstructedAccount);
        }
    }
    
    /**
     * Demonstrates concurrency control and optimistic locking.
     */
    private void demonstrateConcurrencyControl() throws Exception {
        System.out.println("Demonstrating Concurrency Control:");
        System.out.println("----------------------------------");
        
        List<BankAccount> accounts = loadAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found. Cannot demonstrate concurrency control.");
            return;
        }
        
        BankAccount account = accounts.get(0);
        String accountId = account.getId();
        
        // Load the account again to simulate concurrent access
        List<DomainEvent> events = eventStore.getEvents(accountId).get();
        BankAccount concurrentAccount = new BankAccount(accountId, events);
        
        // Perform operations on both instances
        account.deposit(new BigDecimal("100.00"), "Deposit 1", "User 1");
        concurrentAccount.deposit(new BigDecimal("200.00"), "Deposit 2", "User 2");
        
        // Save the first account
        saveAccount(account);
        System.out.println("Saved first account with deposit of $100.00");
        
        // Try to save the second account (should fail due to concurrency conflict)
        try {
            saveAccount(concurrentAccount);
            System.out.println("Saved second account with deposit of $200.00");
        } catch (Exception e) {
            System.out.println("Failed to save second account due to concurrency conflict: " + e.getMessage());
        }
        
        // Show final state
        BankAccount finalAccount = loadAccount(accountId);
        System.out.println("Final account state: " + finalAccount);
    }
    
    /**
     * Shows event store statistics and monitoring information.
     */
    private void showEventStoreStatistics() throws Exception {
        System.out.println("Event Store Statistics:");
        System.out.println("----------------------");
        
        InMemoryEventStore inMemoryStore = (InMemoryEventStore) eventStore;
        InMemoryEventStore.EventStoreStats stats = inMemoryStore.getStats().get();
        
        System.out.println("Total events: " + stats.getTotalEvents());
        System.out.println("Total aggregates: " + stats.getTotalAggregates());
        System.out.println("Events per aggregate:");
        stats.getEventsPerAggregate().forEach((aggregateId, eventCount) -> {
            System.out.println("  " + aggregateId + ": " + eventCount + " events");
        });
    }
    
    /**
     * Demonstrates temporal queries and event filtering.
     */
    private void demonstrateTemporalQueries() throws Exception {
        System.out.println("Demonstrating Temporal Queries:");
        System.out.println("-------------------------------");
        
        // Get events from the last hour
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<DomainEvent> recentEvents = eventStore.getEventsFromTime(oneHourAgo).get();
        
        System.out.println("Events from the last hour: " + recentEvents.size());
        recentEvents.forEach(event -> {
            System.out.println("  " + event.getEventType() + " at " + event.getOccurredAt());
        });
        
        // Show transaction history for a specific time range
        List<BankAccount> accounts = loadAllAccounts();
        if (!accounts.isEmpty()) {
            String accountId = accounts.get(0).getId();
            Instant startTime = Instant.now().minusSeconds(7200); // 2 hours ago
            Instant endTime = Instant.now();
            
            List<TransactionHistoryProjection.TransactionRecord> transactions = 
                transactionProjection.getTransactionsForAccount(accountId, startTime, endTime);
            
            System.out.println("\nTransactions for account " + accountId + " in the last 2 hours:");
            transactions.forEach(transaction -> {
                System.out.println("  " + transaction);
            });
        }
    }
    
    /**
     * Saves an account to the event store.
     */
    private void saveAccount(BankAccount account) throws Exception {
        List<DomainEvent> uncommittedEvents = account.getUncommittedEvents();
        if (!uncommittedEvents.isEmpty()) {
            long expectedVersion = account.getVersion() - uncommittedEvents.size();
            // For new accounts, expected version should be -1 (aggregate doesn't exist)
            if (expectedVersion == 0) {
                expectedVersion = -1;
            }
            eventStore.appendEvents(account.getId(), expectedVersion, uncommittedEvents).get();
            account.markEventsAsCommitted();
        }
    }
    
    /**
     * Loads an account from the event store.
     */
    private BankAccount loadAccount(String accountId) throws Exception {
        List<DomainEvent> events = eventStore.getEvents(accountId).get();
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return new BankAccount(accountId, events);
    }
    
    /**
     * Loads all accounts from the event store.
     */
    private List<BankAccount> loadAllAccounts() throws Exception {
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        // Group events by aggregate ID
        Map<String, List<DomainEvent>> eventsByAccount = new HashMap<>();
        for (DomainEvent event : allEvents) {
            eventsByAccount.computeIfAbsent(event.getAggregateId(), k -> new ArrayList<>()).add(event);
        }
        
        // Create account instances
        List<BankAccount> accounts = new ArrayList<>();
        for (Map.Entry<String, List<DomainEvent>> entry : eventsByAccount.entrySet()) {
            accounts.add(new BankAccount(entry.getKey(), entry.getValue()));
        }
        
        return accounts;
    }
    
    /**
     * Updates all projections with the latest events.
     */
    private void updateProjections() throws Exception {
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        // Reset projections
        balanceProjection.reset();
        transactionProjection.reset();
        
        // Replay all events
        for (DomainEvent event : allEvents) {
            balanceProjection.processEvent(event);
            transactionProjection.processEvent(event);
        }
    }
    
    /**
     * Main method to run the demo.
     */
    public static void main(String[] args) {
        EventSourcingDemo demo = new EventSourcingDemo();
        demo.runDemo();
    }
}
