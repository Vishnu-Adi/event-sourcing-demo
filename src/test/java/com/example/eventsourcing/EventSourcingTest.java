package com.example.eventsourcing;

import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.core.EventStore;
import com.example.eventsourcing.domain.account.BankAccount;
import com.example.eventsourcing.domain.account.InsufficientFundsException;
import com.example.eventsourcing.projection.AccountBalanceProjection;
import com.example.eventsourcing.projection.TransactionHistoryProjection;
import com.example.eventsourcing.store.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for the Event Sourcing implementation.
 * 
 * This test suite demonstrates and validates:
 * 1. Basic Event Sourcing operations
 * 2. Aggregate behavior and state management
 * 3. Event store functionality
 * 4. Projection behavior
 * 5. Error handling and edge cases
 */
public class EventSourcingTest {
    
    private EventStore eventStore;
    private AccountBalanceProjection balanceProjection;
    private TransactionHistoryProjection transactionProjection;
    
    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore();
        balanceProjection = new AccountBalanceProjection("TestBalanceProjection");
        transactionProjection = new TransactionHistoryProjection("TestTransactionProjection");
    }
    
    @Test
    void testAccountCreation() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        
        // When
        saveAccount(account);
        
        // Then
        assertThat(account.getAccountHolderName()).isEqualTo("John Doe");
        assertThat(account.getAccountType()).isEqualTo("CHECKING");
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(account.isClosed()).isFalse();
        assertThat(account.getVersion()).isEqualTo(1);
        assertThat(account.getUncommittedEvents()).hasSize(0); // Events are committed after save
        
        // Verify event was stored
        List<DomainEvent> events = eventStore.getEvents(account.getId()).get();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("AccountOpened");
    }
    
    @Test
    void testMoneyDeposit() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        // When
        String transactionId = account.deposit(new BigDecimal("500.00"), "Salary deposit", "John Doe");
        saveAccount(account);
        
        // Then
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(transactionId).isNotNull();
        assertThat(account.getVersion()).isEqualTo(2);
        
        // Verify events
        List<DomainEvent> events = eventStore.getEvents(account.getId()).get();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("AccountOpened");
        assertThat(events.get(1).getEventType()).isEqualTo("MoneyDeposited");
    }
    
    @Test
    void testMoneyWithdrawal() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        // When
        String transactionId = account.withdraw(new BigDecimal("300.00"), "Grocery shopping", "John Doe");
        saveAccount(account);
        
        // Then
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("700.00"));
        assertThat(transactionId).isNotNull();
        assertThat(account.getVersion()).isEqualTo(2);
        
        // Verify events
        List<DomainEvent> events = eventStore.getEvents(account.getId()).get();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("AccountOpened");
        assertThat(events.get(1).getEventType()).isEqualTo("MoneyWithdrawn");
    }
    
    @Test
    void testInsufficientFunds() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        // When & Then
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("1500.00"), "Large withdrawal", "John Doe"))
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("Insufficient funds")
            .hasMessageContaining("requested 1500.00")
            .hasMessageContaining("only 1000.00 available");
        
        // Verify balance unchanged
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(account.getVersion()).isEqualTo(1); // No new events
    }
    
    @Test
    void testAccountClosure() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        // When
        account.close("Account holder requested closure", "John Doe", "ACCOUNT_456");
        saveAccount(account);
        
        // Then
        assertThat(account.isClosed()).isTrue();
        assertThat(account.getClosedReason()).isEqualTo("Account holder requested closure");
        assertThat(account.getVersion()).isEqualTo(2);
        
        // Verify events
        List<DomainEvent> events = eventStore.getEvents(account.getId()).get();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("AccountOpened");
        assertThat(events.get(1).getEventType()).isEqualTo("AccountClosed");
    }
    
    @Test
    void testEventReplay() throws Exception {
        // Given
        BankAccount originalAccount = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(originalAccount);
        
        originalAccount.deposit(new BigDecimal("500.00"), "Salary", "John Doe");
        saveAccount(originalAccount);
        
        originalAccount.withdraw(new BigDecimal("200.00"), "Shopping", "John Doe");
        saveAccount(originalAccount);
        
        // When
        List<DomainEvent> events = eventStore.getEvents(originalAccount.getId()).get();
        BankAccount reconstructedAccount = new BankAccount(originalAccount.getId(), events);
        
        // Then
        assertThat(reconstructedAccount.getId()).isEqualTo(originalAccount.getId());
        assertThat(reconstructedAccount.getAccountHolderName()).isEqualTo("John Doe");
        assertThat(reconstructedAccount.getAccountType()).isEqualTo("CHECKING");
        assertThat(reconstructedAccount.getBalance()).isEqualTo(new BigDecimal("1300.00"));
        assertThat(reconstructedAccount.isClosed()).isFalse();
        assertThat(reconstructedAccount.getVersion()).isEqualTo(3);
    }
    
    @Test
    void testBalanceProjection() throws Exception {
        // Given
        BankAccount account1 = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        BankAccount account2 = new BankAccount("Jane Smith", "SAVINGS", new BigDecimal("5000.00"));
        
        saveAccount(account1);
        saveAccount(account2);
        
        account1.deposit(new BigDecimal("500.00"), "Salary", "John Doe");
        account2.withdraw(new BigDecimal("1000.00"), "Emergency", "Jane Smith");
        
        saveAccount(account1);
        saveAccount(account2);
        
        // When
        updateProjections();
        
        // Then
        assertThat(balanceProjection.getTotalBalance()).isEqualTo(new BigDecimal("5500.00"));
        assertThat(balanceProjection.getTotalBalanceByType("CHECKING")).isEqualTo(new BigDecimal("1500.00"));
        assertThat(balanceProjection.getTotalBalanceByType("SAVINGS")).isEqualTo(new BigDecimal("4000.00"));
        
        AccountBalanceProjection.AccountBalance johnBalance = balanceProjection.getAccountBalance(account1.getId());
        assertThat(johnBalance).isNotNull();
        assertThat(johnBalance.getBalance()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(johnBalance.getAccountHolderName()).isEqualTo("John Doe");
        assertThat(johnBalance.getAccountType()).isEqualTo("CHECKING");
        assertThat(johnBalance.isClosed()).isFalse();
    }
    
    @Test
    void testTransactionHistoryProjection() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        String depositId = account.deposit(new BigDecimal("500.00"), "Salary", "John Doe");
        String withdrawalId = account.withdraw(new BigDecimal("200.00"), "Shopping", "John Doe");
        
        saveAccount(account);
        
        // When
        updateProjections();
        
        // Then
        assertThat(transactionProjection.getTotalTransactionCount()).isEqualTo(3); // AccountOpened + Deposit + Withdrawal
        
        List<TransactionHistoryProjection.TransactionRecord> accountTransactions = 
            transactionProjection.getTransactionsForAccount(account.getId());
        assertThat(accountTransactions).hasSize(3);
        
        assertThat(transactionProjection.getTransactionsByType("DEPOSIT")).hasSize(1);
        assertThat(transactionProjection.getTransactionsByType("WITHDRAWAL")).hasSize(1);
        assertThat(transactionProjection.getTransactionsByType("ACCOUNT_OPENED")).hasSize(1);
        
        TransactionHistoryProjection.TransactionRecord depositTransaction = 
            transactionProjection.getTransactionById(depositId);
        assertThat(depositTransaction).isNotNull();
        assertThat(depositTransaction.getAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(depositTransaction.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(depositTransaction.getDescription()).isEqualTo("Salary");
    }
    
    @Test
    void testEventStoreStatistics() throws Exception {
        // Given
        BankAccount account1 = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        BankAccount account2 = new BankAccount("Jane Smith", "SAVINGS", new BigDecimal("5000.00"));
        
        saveAccount(account1);
        saveAccount(account2);
        
        account1.deposit(new BigDecimal("500.00"), "Salary", "John Doe");
        account2.withdraw(new BigDecimal("1000.00"), "Emergency", "Jane Smith");
        
        saveAccount(account1);
        saveAccount(account2);
        
        // When
        InMemoryEventStore inMemoryStore = (InMemoryEventStore) eventStore;
        InMemoryEventStore.EventStoreStats stats = inMemoryStore.getStats().get();
        
        // Then
        assertThat(stats.getTotalEvents()).isEqualTo(4); // 2 AccountOpened + 1 Deposit + 1 Withdrawal
        assertThat(stats.getTotalAggregates()).isEqualTo(2);
        assertThat(stats.getEventsPerAggregate()).hasSize(2);
        assertThat(stats.getEventsPerAggregate().get(account1.getId())).isEqualTo(2);
        assertThat(stats.getEventsPerAggregate().get(account2.getId())).isEqualTo(2);
    }
    
    @Test
    void testTemporalQueries() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        Thread.sleep(100); // Ensure different timestamps
        
        account.deposit(new BigDecimal("500.00"), "Salary", "John Doe");
        saveAccount(account);
        
        Thread.sleep(100);
        
        account.withdraw(new BigDecimal("200.00"), "Shopping", "John Doe");
        saveAccount(account);
        
        // When
        List<DomainEvent> allEvents = eventStore.getAllEvents().get();
        Instant midTime = allEvents.get(1).getOccurredAt();
        List<DomainEvent> eventsFromMidTime = eventStore.getEventsFromTime(midTime).get();
        
        // Then
        assertThat(allEvents).hasSize(3);
        assertThat(eventsFromMidTime).hasSize(2); // Deposit + Withdrawal
    }
    
    @Test
    void testProjectionReset() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        updateProjections();
        
        assertThat(balanceProjection.getTotalBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(transactionProjection.getTotalTransactionCount()).isEqualTo(1);
        
        // When
        balanceProjection.reset();
        transactionProjection.reset();
        
        // Then
        assertThat(balanceProjection.getTotalBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(transactionProjection.getTotalTransactionCount()).isEqualTo(0);
    }
    
    @Test
    void testInvalidOperations() throws Exception {
        // Given
        BankAccount account = new BankAccount("John Doe", "CHECKING", new BigDecimal("1000.00"));
        saveAccount(account);
        
        // When & Then - Try to deposit negative amount
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-100.00"), "Invalid deposit", "John Doe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deposit amount must be positive");
        
        // When & Then - Try to withdraw negative amount
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-100.00"), "Invalid withdrawal", "John Doe"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Withdrawal amount must be positive");
        
        // When & Then - Try to deposit to closed account
        account.close("Test closure", "John Doe", null);
        saveAccount(account);
        
        assertThatThrownBy(() -> account.deposit(new BigDecimal("100.00"), "Deposit to closed account", "John Doe"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot deposit to a closed account");
    }
    
    // Helper methods
    
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
}
