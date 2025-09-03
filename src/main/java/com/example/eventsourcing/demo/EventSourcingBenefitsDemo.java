package com.example.eventsourcing.demo;

import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.core.EventStore;
import com.example.eventsourcing.domain.account.BankAccount;
import com.example.eventsourcing.projection.AccountBalanceProjection;
import com.example.eventsourcing.projection.TransactionHistoryProjection;
import com.example.eventsourcing.store.InMemoryEventStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstration of the key benefits of Event Sourcing.
 * 
 * This demo specifically showcases:
 * 1. Complete Audit Trail - Every change is recorded
 * 2. Temporal Queries - Query state at any point in time
 * 3. Event Replay - Rebuild state from events
 * 4. Flexible Read Models - Multiple projections from same events
 * 5. Debugging and Troubleshooting - See exactly what happened
 * 6. Compliance and Regulatory - Complete history for audits
 * 
 * These benefits are difficult or impossible to achieve with traditional
 * CRUD-based systems where data is overwritten.
 */
public class EventSourcingBenefitsDemo {
    
    private final EventStore eventStore;
    private final AccountBalanceProjection balanceProjection;
    private final TransactionHistoryProjection transactionProjection;
    
    /**
     * Constructor for creating a new benefits demonstration.
     */
    public EventSourcingBenefitsDemo() {
        this.eventStore = new InMemoryEventStore();
        this.balanceProjection = new AccountBalanceProjection("BenefitsDemoBalanceProjection");
        this.transactionProjection = new TransactionHistoryProjection("BenefitsDemoTransactionProjection");
    }
    
    /**
     * Runs the complete benefits demonstration.
     */
    public void runDemo() {
        System.out.println("=== Event Sourcing Benefits Demo ===\n");
        
        try {
            // Step 1: Create a scenario with multiple transactions
            System.out.println("Step 1: Creating a Complex Scenario");
            System.out.println("===================================");
            createComplexScenario();
            
            // Step 2: Demonstrate complete audit trail
            System.out.println("\nStep 2: Complete Audit Trail");
            System.out.println("============================");
            demonstrateAuditTrail();
            
            // Step 3: Demonstrate temporal queries
            System.out.println("\nStep 3: Temporal Queries");
            System.out.println("========================");
            demonstrateTemporalQueries();
            
            // Step 4: Demonstrate event replay
            System.out.println("\nStep 4: Event Replay and State Reconstruction");
            System.out.println("=============================================");
            demonstrateEventReplay();
            
            // Step 5: Demonstrate flexible read models
            System.out.println("\nStep 5: Flexible Read Models");
            System.out.println("============================");
            demonstrateFlexibleReadModels();
            
            // Step 6: Demonstrate debugging capabilities
            System.out.println("\nStep 6: Debugging and Troubleshooting");
            System.out.println("=====================================");
            demonstrateDebuggingCapabilities();
            
            // Step 7: Demonstrate compliance features
            System.out.println("\nStep 7: Compliance and Regulatory Features");
            System.out.println("==========================================");
            demonstrateComplianceFeatures();
            
        } catch (Exception e) {
            System.err.println("Demo failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a complex scenario with multiple accounts and transactions.
     */
    private void createComplexScenario() throws Exception {
        // Create accounts at different times
    BankAccount account1 = new BankAccount("Alice Johnson", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account1);
        System.out.println("Created account for Alice Johnson: " + account1);
        
        // Wait a bit to simulate time passing
        Thread.sleep(100);
        
    BankAccount account2 = new BankAccount("Bob Smith", "SAVINGS", new BigDecimal("5000.00"));
        saveAccount(account2);
        System.out.println("Created account for Bob Smith: " + account2);
        
        // Perform various transactions over time
        Thread.sleep(100);
        account1.deposit(new BigDecimal("500.00"), "Salary deposit", "Alice Johnson");
        saveAccount(account1);
    System.out.println("Alice deposited INR 500.00");
        
        Thread.sleep(100);
        account2.withdraw(new BigDecimal("1000.00"), "Emergency fund", "Bob Smith");
        saveAccount(account2);
    System.out.println("Bob withdrew INR 1000.00");
        
        Thread.sleep(100);
        account1.withdraw(new BigDecimal("200.00"), "Grocery shopping", "Alice Johnson");
        saveAccount(account1);
    System.out.println("Alice withdrew INR 200.00");
        
        Thread.sleep(100);
        account2.deposit(new BigDecimal("2500.00"), "Investment return", "Bob Smith");
        saveAccount(account2);
    System.out.println("Bob deposited INR 2500.00");
        
        Thread.sleep(100);
        account1.deposit(new BigDecimal("100.00"), "Cashback reward", "Alice Johnson");
        saveAccount(account1);
    System.out.println("Alice deposited INR 100.00");
        
        // Update projections
        updateProjections();
    }
    
    /**
     * Demonstrates the complete audit trail capability.
     */
    private void demonstrateAuditTrail() throws Exception {
        System.out.println("Complete Audit Trail - Every Change is Recorded:");
        System.out.println("------------------------------------------------");
        
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        System.out.println("Total events recorded: " + allEvents.size());
        
        System.out.println("\nComplete event history:");
        for (int i = 0; i < allEvents.size(); i++) {
            DomainEvent event = allEvents.get(i);
            System.out.println(String.format("%2d. %s", i + 1, event));
        }
        
        System.out.println("\nKey Benefits:");
        System.out.println("- Every change is permanently recorded");
        System.out.println("- No data is ever lost or overwritten");
        System.out.println("- Complete history for compliance and auditing");
        System.out.println("- Events are immutable once stored");
    }
    
    /**
     * Demonstrates temporal queries - querying state at different points in time.
     */
    private void demonstrateTemporalQueries() throws Exception {
        System.out.println("Temporal Queries - Query State at Any Point in Time:");
        System.out.println("---------------------------------------------------");
        
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        if (allEvents.size() < 3) {
            System.out.println("Not enough events to demonstrate temporal queries.");
            return;
        }
        
        // Get events at different time points
        Instant time1 = allEvents.get(1).getOccurredAt();
        Instant time2 = allEvents.get(3).getOccurredAt();
        Instant time3 = allEvents.get(5).getOccurredAt();
        
        System.out.println("Events at time 1 (" + time1 + "):");
        List<DomainEvent> eventsAtTime1 = eventStore.getEventsFromTime(time1).get();
        eventsAtTime1.forEach(event -> System.out.println("  " + event.getEventType()));
        
        System.out.println("\nEvents at time 2 (" + time2 + "):");
        List<DomainEvent> eventsAtTime2 = eventStore.getEventsFromTime(time2).get();
        eventsAtTime2.forEach(event -> System.out.println("  " + event.getEventType()));
        
        System.out.println("\nEvents at time 3 (" + time3 + "):");
        List<DomainEvent> eventsAtTime3 = eventStore.getEventsFromTime(time3).get();
        eventsAtTime3.forEach(event -> System.out.println("  " + event.getEventType()));
        
        System.out.println("\nKey Benefits:");
        System.out.println("- Query state at any point in time");
        System.out.println("- Analyze trends and patterns over time");
        System.out.println("- Replay events to understand what happened");
        System.out.println("- Debug issues by examining historical state");
    }
    
    /**
     * Demonstrates event replay and state reconstruction.
     */
    private void demonstrateEventReplay() throws Exception {
        System.out.println("Event Replay and State Reconstruction:");
        System.out.println("-------------------------------------");
        
        List<BankAccount> accounts = loadAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        
        String accountId = accounts.get(0).getId();
        List<DomainEvent> accountEvents = eventStore.getEvents(accountId).get();
        
        System.out.println("Reconstructing account " + accountId + " from events:");
        System.out.println("Total events for this account: " + accountEvents.size());
        
        // Replay events one by one to show state evolution
        BankAccount reconstructedAccount = new BankAccount(accountId, accountEvents);
        System.out.println("Final reconstructed state: " + reconstructedAccount);
        
        // Show how state evolves with each event using actual amounts
        System.out.println("\nState evolution with each event:");
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (int i = 0; i < accountEvents.size(); i++) {
            DomainEvent event = accountEvents.get(i);
            System.out.println(String.format("Event %d: %s", i + 1, event.getEventType()));
            switch (event.getEventType()) {
                case "AccountOpened":
                    runningBalance = ((com.example.eventsourcing.domain.account.AccountOpened) event).getInitialBalance();
                    break;
                case "MoneyDeposited":
                    runningBalance = ((com.example.eventsourcing.domain.account.MoneyDeposited) event).getNewBalance();
                    break;
                case "MoneyWithdrawn":
                    runningBalance = ((com.example.eventsourcing.domain.account.MoneyWithdrawn) event).getNewBalance();
                    break;
                default:
                    break;
            }
            System.out.println("  Balance after this event: INR " + runningBalance);
        }
        
        System.out.println("\nKey Benefits:");
        System.out.println("- State can be reconstructed from events");
        System.out.println("- No need to store current state separately");
        System.out.println("- Events are the single source of truth");
        System.out.println("- Can replay events to any point in time");
    }
    
    /**
     * Demonstrates flexible read models and projections.
     */
    private void demonstrateFlexibleReadModels() throws Exception {
        System.out.println("Flexible Read Models - Multiple Views from Same Events:");
        System.out.println("-----------------------------------------------------");
        
        // Show different projections built from the same events
        System.out.println("1. Account Balance Projection:");
        System.out.println("   - Current balance for each account");
        System.out.println("   - Total balances by account type");
        System.out.println("   - Account status (open/closed)");
        
        balanceProjection.getAllAccountBalances().forEach((accountId, balance) -> {
            System.out.println("   " + balance);
        });
        
        System.out.println("\n2. Transaction History Projection:");
        System.out.println("   - Complete transaction history");
        System.out.println("   - Transactions by type and date");
        System.out.println("   - Transaction statistics");
        
        System.out.println("   Total transactions: " + transactionProjection.getTotalTransactionCount());
        System.out.println("   Deposits: " + transactionProjection.getTransactionsByType("DEPOSIT").size());
        System.out.println("   Withdrawals: " + transactionProjection.getTransactionsByType("WITHDRAWAL").size());
        
        System.out.println("\n3. Potential Additional Projections:");
        System.out.println("   - Monthly transaction summaries");
        System.out.println("   - Account activity reports");
        System.out.println("   - Risk assessment models");
        System.out.println("   - Customer behavior analytics");
        
        System.out.println("\nKey Benefits:");
        System.out.println("- Multiple read models from same event stream");
        System.out.println("- Optimized views for different use cases");
        System.out.println("- Easy to add new projections without changing core logic");
        System.out.println("- Read models can be rebuilt from events");
    }
    
    /**
     * Demonstrates debugging and troubleshooting capabilities.
     */
    private void demonstrateDebuggingCapabilities() throws Exception {
        System.out.println("Debugging and Troubleshooting Capabilities:");
        System.out.println("------------------------------------------");
        
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        System.out.println("1. Event Sequence Analysis:");
        System.out.println("   - See exactly what happened and when");
        System.out.println("   - Identify patterns and anomalies");
        System.out.println("   - Trace the cause of issues");
        
        System.out.println("\n2. State Reconstruction at Any Point:");
        System.out.println("   - Rebuild state to any point in time");
        System.out.println("   - Compare state before and after changes");
        System.out.println("   - Identify when issues occurred");
        
        System.out.println("\n3. Event Correlation:");
        System.out.println("   - See how events relate to each other");
        System.out.println("   - Identify cause-and-effect relationships");
        System.out.println("   - Understand system behavior");
        
        // Show event correlation example
        System.out.println("\nExample: Event Correlation Analysis");
        for (int i = 0; i < allEvents.size() - 1; i++) {
            DomainEvent currentEvent = allEvents.get(i);
            DomainEvent nextEvent = allEvents.get(i + 1);
            
            if (currentEvent.getAggregateId().equals(nextEvent.getAggregateId())) {
                System.out.println("  " + currentEvent.getEventType() + " -> " + nextEvent.getEventType() + 
                                 " (Account: " + currentEvent.getAggregateId() + ")");
            }
        }
        
        System.out.println("\nKey Benefits:");
        System.out.println("- Complete visibility into system behavior");
        System.out.println("- Easy to identify and fix issues");
        System.out.println("- Can replay events to reproduce problems");
        System.out.println("- No data loss during debugging");
    }
    
    /**
     * Demonstrates compliance and regulatory features.
     */
    private void demonstrateComplianceFeatures() throws Exception {
        System.out.println("Compliance and Regulatory Features:");
        System.out.println("----------------------------------");
        
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        System.out.println("1. Complete Audit Trail:");
        System.out.println("   - Every change is permanently recorded");
        System.out.println("   - No data can be modified or deleted");
        System.out.println("   - Events are immutable once stored");
        
        System.out.println("\n2. Regulatory Reporting:");
        System.out.println("   - Generate reports for any time period");
        System.out.println("   - Show complete transaction history");
        System.out.println("   - Demonstrate compliance with regulations");
        
        System.out.println("\n3. Data Integrity:");
        System.out.println("   - Events are cryptographically signed");
        System.out.println("   - Tamper detection and prevention");
        System.out.println("   - Immutable event log");
        
        System.out.println("\n4. Retention and Archival:");
        System.out.println("   - Events can be archived for long-term storage");
        System.out.println("   - Historical data is always available");
        System.out.println("   - Compliance with data retention policies");
        
        // Show compliance report example
        System.out.println("\nExample: Compliance Report");
        System.out.println("Total events: " + allEvents.size());
        System.out.println("Date range: " + allEvents.get(0).getOccurredAt() + " to " + 
                         allEvents.get(allEvents.size() - 1).getOccurredAt());
        
        long accountOpenings = allEvents.stream()
            .mapToLong(event -> "AccountOpened".equals(event.getEventType()) ? 1 : 0)
            .sum();
        System.out.println("Account openings: " + accountOpenings);
        
        long deposits = allEvents.stream()
            .mapToLong(event -> "MoneyDeposited".equals(event.getEventType()) ? 1 : 0)
            .sum();
        System.out.println("Deposits: " + deposits);
        
        long withdrawals = allEvents.stream()
            .mapToLong(event -> "MoneyWithdrawn".equals(event.getEventType()) ? 1 : 0)
            .sum();
        System.out.println("Withdrawals: " + withdrawals);
        
        System.out.println("\nKey Benefits:");
        System.out.println("- Complete compliance with regulatory requirements");
        System.out.println("- Immutable audit trail");
        System.out.println("- Easy to generate compliance reports");
        System.out.println("- Data integrity and tamper detection");
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
     * Main method to run the benefits demo.
     */
    public static void main(String[] args) {
        EventSourcingBenefitsDemo demo = new EventSourcingBenefitsDemo();
        demo.runDemo();
    }
}
