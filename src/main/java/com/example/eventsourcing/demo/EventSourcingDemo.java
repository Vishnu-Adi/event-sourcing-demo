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

public class EventSourcingDemo {
    
    private final EventStore eventStore;
    private final AccountBalanceProjection balanceProjection;
    private final TransactionHistoryProjection transactionProjection;
    
    public EventSourcingDemo() {
        this.eventStore = new InMemoryEventStore();
        this.balanceProjection = new AccountBalanceProjection("AccountBalanceProjection");
        this.transactionProjection = new TransactionHistoryProjection("TransactionHistoryProjection");
    }
    
    public void runDemo() {
        System.out.println("=== Event Sourcing Demo ===\n");
        
        try {
            System.out.println("Step 1: Creating Bank Accounts");
            System.out.println("==============================");
            createAccounts();
            
            System.out.println("\nStep 2: Performing Transactions");
            System.out.println("===============================");
            performTransactions();
            
            System.out.println("\nStep 3: Building and Querying Projections");
            System.out.println("=========================================");
            demonstrateProjections();
            
            System.out.println("\nStep 4: Event Replay and State Reconstruction");
            System.out.println("=============================================");
            demonstrateEventReplay();
            
            System.out.println("\nStep 5: Event Store Statistics");
            System.out.println("==============================");
            showEventStoreStatistics();
            
            System.out.println("\nStep 6: Temporal Queries");
            System.out.println("========================");
            demonstrateTemporalQueries();
            
        } catch (Exception e) {
            System.err.println("Demo failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runInteractive() {
        System.out.println("=== Event Sourcing Demo (Interactive) ===\n");
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println();
                System.out.println("Choose an option:");
                System.out.println("1) Create account");
                System.out.println("2) List accounts");
                System.out.println("3) Deposit");
                System.out.println("4) Withdraw");
                System.out.println("5) Close account");
                System.out.println("6) Show projections");
                System.out.println("7) Transfer between accounts");
                System.out.println("8) List all events");
                System.out.println("9) Replay account from events");
                System.out.println("10) Show event store stats");
                System.out.println("11) Temporal query (events from last N seconds)");
                System.out.println("12) View account transactions");
                System.out.println("0) Exit");
                System.out.print("> ");

                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1":
                            interactiveCreateAccount(scanner);
                            promptContinue(scanner);
                            break;
                        case "2":
                            interactiveListAccounts();
                            promptContinue(scanner);
                            break;
                        case "3":
                            interactiveDeposit(scanner);
                            promptContinue(scanner);
                            break;
                        case "4":
                            interactiveWithdraw(scanner);
                            promptContinue(scanner);
                            break;
                        case "5":
                            interactiveCloseAccount(scanner);
                            promptContinue(scanner);
                            break;
                        case "6":
                            demonstrateProjections();
                            promptContinue(scanner);
                            break;
                        case "7":
                            interactiveTransfer(scanner);
                            promptContinue(scanner);
                            break;
                        case "8":
                            interactiveListAllEvents();
                            promptContinue(scanner);
                            break;
                        case "9":
                            interactiveReplayAccount(scanner);
                            promptContinue(scanner);
                            break;
                        case "10":
                            showEventStoreStatistics();
                            promptContinue(scanner);
                            break;
                        case "11":
                            interactiveTemporalQuery(scanner);
                            promptContinue(scanner);
                            break;
                        case "12":
                            interactiveViewTransactions(scanner);
                            promptContinue(scanner);
                            break;
                        case "0":
                            running = false;
                            break;
                        default:
                            System.out.println("Invalid option");
                    }
                } catch (Exception e) {
                    System.out.println("Operation failed: " + e.getMessage());
                }
            }
        }
    }

    private void interactiveCreateAccount(Scanner scanner) throws Exception {
        System.out.print("Account holder name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Account type (CHECKING/SAVINGS): ");
        String type = scanner.nextLine().trim().toUpperCase(Locale.ROOT);
        System.out.print("Initial balance: ");
        BigDecimal initial = new BigDecimal(scanner.nextLine().trim());
        BankAccount account = new BankAccount(name, type, initial);
        saveAccount(account);
        updateProjections();
        System.out.println("Created account: " + account.getId());
    }

    private void interactiveListAccounts() throws Exception {
        // Prefer projection for quick summary
        updateProjections();
        Map<String, AccountBalanceProjection.AccountBalance> all = balanceProjection.getAllAccountBalances();
        if (all.isEmpty()) {
            System.out.println("No accounts found");
            return;
        }
        all.values().forEach(b -> System.out.println(b));
    }

    private void interactiveTransfer(Scanner scanner) throws Exception {
        System.out.print("From Account ID: ");
        String fromId = scanner.nextLine().trim();
        System.out.print("To Account ID: ");
        String toId = scanner.nextLine().trim();
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();
        System.out.print("Performed by: ");
        String by = scanner.nextLine().trim();

        if (fromId.equals(toId)) {
            System.out.println("Cannot transfer to the same account");
            return;
        }

        BankAccount from = loadAccount(fromId);
        BankAccount to = loadAccount(toId);

        // Single currency system (INR), no currency check required

        // Use a single transaction id for correlation across withdraw and deposit
        String txId = UUID.randomUUID().toString();
        try {
            from.withdraw(amount, "Transfer out: " + desc, by, txId);
            to.deposit(amount, "Transfer in: " + desc, by, txId);
            // Save both; if second save fails due to concurrency, caller can retry from latest events
            saveAccount(from);
            saveAccount(to);
            updateProjections();
            System.out.println("Transfer completed. Transaction ID: " + txId);
        } catch (InsufficientFundsException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private void interactiveDeposit(Scanner scanner) throws Exception {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        BankAccount account = loadAccount(accountId);
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();
        System.out.print("Deposited by: ");
        String by = scanner.nextLine().trim();
        String txId = account.deposit(amount, desc, by);
        saveAccount(account);
        updateProjections();
        System.out.println("Deposit recorded. Transaction ID: " + txId);
    }

    private void interactiveWithdraw(Scanner scanner) throws Exception {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        BankAccount account = loadAccount(accountId);
        System.out.print("Amount: ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();
        System.out.print("Withdrawn by: ");
        String by = scanner.nextLine().trim();
        try {
            String txId = account.withdraw(amount, desc, by);
            saveAccount(account);
            updateProjections();
            System.out.println("Withdrawal recorded. Transaction ID: " + txId);
        } catch (InsufficientFundsException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private void interactiveCloseAccount(Scanner scanner) throws Exception {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        BankAccount account = loadAccount(accountId);
        System.out.print("Reason: ");
        String reason = scanner.nextLine().trim();
        System.out.print("Closed by: ");
        String by = scanner.nextLine().trim();
        System.out.print("Transfer remaining funds to account ID (optional, blank to skip): ");
        String transferTo = scanner.nextLine().trim();
        if (transferTo.isEmpty()) transferTo = null;
        account.close(reason, by, transferTo);
        saveAccount(account);
        updateProjections();
        System.out.println("Account closed");
    }

    private void interactiveListAllEvents() throws Exception {
        List<DomainEvent> all = eventStore.getAllEvents().get();
        if (all.isEmpty()) {
            System.out.println("No events recorded");
            return;
        }
        for (int i = 0; i < all.size(); i++) {
            System.out.println((i + 1) + ". " + all.get(i));
        }
    }

    private void interactiveReplayAccount(Scanner scanner) throws Exception {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        List<DomainEvent> accountEvents = eventStore.getEvents(accountId).get();
        if (accountEvents.isEmpty()) {
            System.out.println("No events for account");
            return;
        }
        BankAccount reconstructed = new BankAccount(accountId, accountEvents);
        System.out.println("Reconstructed: " + reconstructed);
    }

    private void interactiveTemporalQuery(Scanner scanner) throws Exception {
        System.out.print("Last N seconds: ");
        String s = scanner.nextLine().trim();
        long seconds = Long.parseLong(s);
        Instant from = Instant.now().minusSeconds(seconds);
        List<DomainEvent> events = eventStore.getEventsFromTime(from).get();
        System.out.println("Events found: " + events.size());
        for (DomainEvent e : events) {
            System.out.println(e.getOccurredAt() + " - " + e.getEventType() + " - " + e.getAggregateId());
        }
    }

    private void promptContinue(Scanner scanner) {
        System.out.print("Continue? (y/n): ");
        String ans = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (!ans.equals("y") && !ans.equals("yes")) {
            // Exit by throwing a runtime to unwind to main and stop loop
            throw new RuntimeException("exit-interactive");
        }
    }

    private void interactiveViewTransactions(Scanner scanner) throws Exception {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        updateProjections();
        List<TransactionHistoryProjection.TransactionRecord> txs = transactionProjection.getTransactionsForAccount(accountId);
        if (txs.isEmpty()) {
            System.out.println("No transactions found for this account");
            return;
        }
        System.out.println("Transactions for account " + accountId + ":");
        for (TransactionHistoryProjection.TransactionRecord t : txs) {
            System.out.println("  " + t);
        }
    }
    
    private void createAccounts() throws Exception {
        BankAccount johnAccount = new BankAccount(
            "John Doe", 
            "CHECKING", 
            new BigDecimal("1000.00")
        );
        
        saveAccount(johnAccount);
        System.out.println("Created account for John Doe: " + johnAccount);
        
        BankAccount janeAccount = new BankAccount(
            "Jane Smith", 
            "SAVINGS", 
            new BigDecimal("5000.00")
        );
        
        saveAccount(janeAccount);
        System.out.println("Created account for Jane Smith: " + janeAccount);
        
        BankAccount bobAccount = new BankAccount(
            "Bob Johnson", 
            "CHECKING", 
            new BigDecimal("2500.00")
        );
        
        saveAccount(bobAccount);
        System.out.println("Created account for Bob Johnson: " + bobAccount);
        
        updateProjections();
    }
    
    private void performTransactions() throws Exception {
        List<BankAccount> accounts = loadAllAccounts();
        
        if (accounts.isEmpty()) {
            System.out.println("No accounts found. Cannot perform transactions.");
            return;
        }
        
        BankAccount johnAccount = accounts.get(0);
        BankAccount janeAccount = accounts.get(1);
        BankAccount bobAccount = accounts.get(2);
        
        String depositId1 = johnAccount.deposit(
            new BigDecimal("500.00"), 
            "Salary deposit", 
            "John Doe"
        );
        saveAccount(johnAccount);
    System.out.println("John deposited INR 500.00 (Transaction ID: " + depositId1 + ")");
        
        String withdrawalId1 = janeAccount.withdraw(
            new BigDecimal("1000.00"), 
            "Vacation fund", 
            "Jane Smith"
        );
        saveAccount(janeAccount);
    System.out.println("Jane withdrew INR 1000.00 (Transaction ID: " + withdrawalId1 + ")");
        
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
    System.out.println("Bob transferred INR 200.00 to John");
        
        try {
            johnAccount.withdraw(
                new BigDecimal("2000.00"), 
                "Large withdrawal", 
                "John Doe"
            );
            saveAccount(johnAccount);
        } catch (InsufficientFundsException e) {
            System.out.println("Failed to withdraw INR 2000.00 from John's account: " + e.getMessage());
        }
        
        updateProjections();
    }
    
    private void demonstrateProjections() throws Exception {
        System.out.println("Account Balance Projection:");
        System.out.println("--------------------------");
        
        balanceProjection.getAllAccountBalances().forEach((accountId, balance) -> {
            System.out.println("  " + balance);
        });
        
    System.out.println("\nTotal balance across all accounts: INR " + balanceProjection.getTotalBalance());
    System.out.println("Total checking accounts balance: INR " + balanceProjection.getTotalBalanceByType("CHECKING"));
    System.out.println("Total savings accounts balance: INR " + balanceProjection.getTotalBalanceByType("SAVINGS"));
        
        System.out.println("\nAccount count by type:");
        balanceProjection.getAccountCountByType().forEach((type, count) -> {
            System.out.println("  " + type + ": " + count + " accounts");
        });
        
        System.out.println("\nTransaction History Projection:");
        System.out.println("-------------------------------");
        
        System.out.println("Total transactions: " + transactionProjection.getTotalTransactionCount());
        
        System.out.println("Deposits: " + transactionProjection.getTransactionsByType("DEPOSIT").size());
        System.out.println("Withdrawals: " + transactionProjection.getTransactionsByType("WITHDRAWAL").size());
        System.out.println("Account openings: " + transactionProjection.getTransactionsByType("ACCOUNT_OPENED").size());
        
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
    
    private void demonstrateEventReplay() throws Exception {
        System.out.println("Demonstrating Event Replay:");
        System.out.println("---------------------------");
        
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        System.out.println("Total events in store: " + allEvents.size());
        
        System.out.println("\nEvents in chronological order:");
        allEvents.forEach(event -> {
            System.out.println("  " + event);
        });
        
        List<BankAccount> accounts = loadAllAccounts();
        if (!accounts.isEmpty()) {
            String accountId = accounts.get(0).getId();
            List<DomainEvent> accountEvents = eventStore.getEvents(accountId).get();
            
            System.out.println("\nReconstructing account " + accountId + " from events:");
            System.out.println("Events for this account: " + accountEvents.size());
            
            BankAccount reconstructedAccount = new BankAccount(accountId, accountEvents);
            System.out.println("Reconstructed account: " + reconstructedAccount);
        }
    }
    
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
    
    private void demonstrateTemporalQueries() throws Exception {
        System.out.println("Demonstrating Temporal Queries:");
        System.out.println("-------------------------------");
        
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<DomainEvent> recentEvents = eventStore.getEventsFromTime(oneHourAgo).get();
        
        System.out.println("Events from the last hour: " + recentEvents.size());
        recentEvents.forEach(event -> {
            System.out.println("  " + event.getEventType() + " at " + event.getOccurredAt());
        });
        
        List<BankAccount> accounts = loadAllAccounts();
        if (!accounts.isEmpty()) {
            String accountId = accounts.get(0).getId();
            Instant startTime = Instant.now().minusSeconds(7200);
            Instant endTime = Instant.now();
            
            List<TransactionHistoryProjection.TransactionRecord> transactions = 
                transactionProjection.getTransactionsForAccount(accountId, startTime, endTime);
            
            System.out.println("\nTransactions for account " + accountId + " in the last 2 hours:");
            transactions.forEach(transaction -> {
                System.out.println("  " + transaction);
            });
        }
    }
    
    private void saveAccount(BankAccount account) throws Exception {
        List<DomainEvent> uncommittedEvents = account.getUncommittedEvents();
        if (!uncommittedEvents.isEmpty()) {
            eventStore.appendEvents(account.getId(), -1, uncommittedEvents).get();
            account.markEventsAsCommitted();
        }
    }
    
    private BankAccount loadAccount(String accountId) throws Exception {
        List<DomainEvent> events = eventStore.getEvents(accountId).get();
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return new BankAccount(accountId, events);
    }
    
    private List<BankAccount> loadAllAccounts() throws Exception {
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        Map<String, List<DomainEvent>> eventsByAccount = new HashMap<>();
        for (DomainEvent event : allEvents) {
            eventsByAccount.computeIfAbsent(event.getAggregateId(), k -> new ArrayList<>()).add(event);
        }
        
        List<BankAccount> accounts = new ArrayList<>();
        for (Map.Entry<String, List<DomainEvent>> entry : eventsByAccount.entrySet()) {
            accounts.add(new BankAccount(entry.getKey(), entry.getValue()));
        }
        
        return accounts;
    }
    
    private void updateProjections() throws Exception {
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        
        balanceProjection.reset();
        transactionProjection.reset();
        
        for (DomainEvent event : allEvents) {
            balanceProjection.processEvent(event);
            transactionProjection.processEvent(event);
        }
    }
    
    public static void main(String[] args) {
        EventSourcingDemo demo = new EventSourcingDemo();
        demo.runInteractive();
    }
}