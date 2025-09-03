# Event Sourcing Demo

A comprehensive demonstration of Event Sourcing principles and implementation using Java. This project showcases how to build a system that stores state as a sequence of events instead of overwriting data.

## Table of Contents

- [Overview](#overview)
- [What is Event Sourcing?](#what-is-event-sourcing)
- [Key Benefits](#key-benefits)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Domain Model](#domain-model)
- [Event Store](#event-store)
- [Projections](#projections)
- [Demo Applications](#demo-applications)
- [Running the Demos](#running-the-demos)
- [Key Concepts Explained](#key-concepts-explained)
- [Best Practices](#best-practices)
- [When to Use Event Sourcing](#when-to-use-event-sourcing)
- [Performance Considerations](#performance-considerations)
- [Testing](#testing)
- [Future Enhancements](#future-enhancements)

## Overview

This project demonstrates Event Sourcing through a banking system example. It includes:

- **Complete Event Sourcing Implementation**: Core interfaces, base classes, and in-memory event store
- **Banking Domain Model**: Account aggregate with deposits, withdrawals, and account management
- **Multiple Projections**: Balance tracking and transaction history
- **Comprehensive Demos**: Two detailed demonstration applications
- **Educational Examples**: Clear examples of Event Sourcing benefits and use cases

## What is Event Sourcing?

Event Sourcing is a pattern where the state of an application is determined by a sequence of events. Instead of storing the current state and overwriting it when changes occur, Event Sourcing:

1. **Stores Events**: Every change is recorded as an immutable event
2. **Replays Events**: Current state is derived by replaying all events
3. **Maintains History**: Complete audit trail of all changes
4. **Enables Queries**: Can query state at any point in time

### Traditional CRUD vs Event Sourcing

**Traditional CRUD:**
```
Account: { id: "123", balance: 1000, holder: "John" }
Update: { id: "123", balance: 1200, holder: "John" }  // Overwrites previous state
```

**Event Sourcing:**
```
Events: [
  AccountOpened { id: "123", initialBalance: 1000, holder: "John" },
  MoneyDeposited { id: "123", amount: 200, newBalance: 1200 }
]
Current State: Derived by replaying events
```

## Key Benefits

### 1. Complete Audit Trail
- Every change is permanently recorded
- No data is ever lost or overwritten
- Complete history for compliance and auditing

### 2. Temporal Queries
- Query state at any point in time
- Analyze trends and patterns over time
- Debug issues by examining historical state

### 3. Event Replay
- State can be reconstructed from events
- Events are the single source of truth
- Can replay events to any point in time

### 4. Flexible Read Models
- Multiple projections from same event stream
- Optimized views for different use cases
- Easy to add new projections without changing core logic

### 5. Debugging and Troubleshooting
- Complete visibility into system behavior
- Easy to identify and fix issues
- Can replay events to reproduce problems

### 6. Compliance and Regulatory
- Complete compliance with regulatory requirements
- Immutable audit trail
- Easy to generate compliance reports

## Project Structure

```
event-sourcing-demo/
├── src/main/java/com/example/eventsourcing/
│   ├── core/                           # Core Event Sourcing interfaces and base classes
│   │   ├── DomainEvent.java            # Base interface for all domain events
│   │   ├── AbstractDomainEvent.java    # Abstract base class for events
│   │   ├── EventStore.java             # Interface for event storage
│   │   ├── AggregateRoot.java          # Base class for aggregate roots
│   │   └── ConcurrencyException.java   # Exception for concurrency conflicts
│   ├── store/                          # Event store implementations
│   │   └── InMemoryEventStore.java     # In-memory event store implementation
│   ├── domain/account/                 # Banking domain model
│   │   ├── BankAccount.java            # Account aggregate root
│   │   ├── AccountOpened.java          # Account opened event
│   │   ├── MoneyDeposited.java         # Money deposited event
│   │   ├── MoneyWithdrawn.java         # Money withdrawn event
│   │   ├── AccountClosed.java          # Account closed event
│   │   └── InsufficientFundsException.java # Domain exception
│   ├── projection/                     # Event projections
│   │   ├── EventProjection.java        # Interface for projections
│   │   ├── AccountBalanceProjection.java # Balance tracking projection
│   │   └── TransactionHistoryProjection.java # Transaction history projection
│   └── demo/                           # Demo applications
│       ├── EventSourcingDemo.java      # Main demonstration
│       └── EventSourcingBenefitsDemo.java # Benefits demonstration
├── pom.xml                             # Maven configuration
└── README.md                           # This file
```

## Core Components

### DomainEvent Interface

The foundation of Event Sourcing - every change is represented as a domain event:

```java
public interface DomainEvent {
    UUID getEventId();           // Unique identifier
    String getAggregateId();     // Aggregate this event belongs to
    long getAggregateVersion();  // Version for concurrency control
    Instant getOccurredAt();     // When the event occurred
    String getEventType();       // Type of event
    long getSequenceNumber();    // Sequence within aggregate
}
```

### EventStore Interface

Defines the contract for storing and retrieving events:

```java
public interface EventStore {
    CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<DomainEvent> events);
    CompletableFuture<List<DomainEvent>> getEvents(String aggregateId);
    CompletableFuture<List<DomainEvent>> getEventsFromVersion(String aggregateId, long fromVersion);
    CompletableFuture<Long> getCurrentVersion(String aggregateId);
    // ... more methods
}
```

### AggregateRoot Base Class

Base class for aggregates that implement Event Sourcing:

```java
public abstract class AggregateRoot {
    protected void applyNewEvent(DomainEvent event);  // Apply new event
    protected abstract void handleEvent(DomainEvent event);  // Handle specific events
    public List<DomainEvent> getUncommittedEvents();  // Get uncommitted events
    public void markEventsAsCommitted();  // Mark events as committed
}
```

## Domain Model

### BankAccount Aggregate

The `BankAccount` class demonstrates a complete Event Sourcing implementation:

```java
public class BankAccount extends AggregateRoot {
    // State derived from events
    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private boolean isClosed;
    
    // Business operations generate events
    public String deposit(BigDecimal amount, String description, String depositedBy) {
        // Business logic validation
        if (isClosed) throw new IllegalStateException("Cannot deposit to closed account");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        
        // Generate event
        MoneyDeposited event = new MoneyDeposited(/* ... */);
        applyNewEvent(event);
        return event.getTransactionId();
    }
    
    // Handle events to update state
    @Override
    protected void handleEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "AccountOpened":
                handleAccountOpened((AccountOpened) event);
                break;
            case "MoneyDeposited":
                handleMoneyDeposited((MoneyDeposited) event);
                break;
            // ... handle other events
        }
    }
}
```

### Domain Events

Each business operation generates a specific event:

- **AccountOpened**: When a new account is created
- **MoneyDeposited**: When money is deposited
- **MoneyWithdrawn**: When money is withdrawn
- **AccountClosed**: When an account is closed

## Event Store

### InMemoryEventStore

A complete in-memory implementation that demonstrates:

- **Thread Safety**: Uses concurrent data structures and locks
- **Optimistic Concurrency Control**: Prevents concurrent modifications
- **Event Ordering**: Maintains proper event sequence
- **Query Capabilities**: Supports various event queries

Key features:
- Atomic event appending
- Version-based concurrency control
- Event replay capabilities
- Statistics and monitoring

## Projections

### AccountBalanceProjection

Maintains current balance for all accounts:

```java
public class AccountBalanceProjection implements EventProjection {
    private final Map<String, AccountBalance> accountBalances;
    
    @Override
    public void processEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "AccountOpened":
                processAccountOpened((AccountOpened) event);
                break;
            case "MoneyDeposited":
                processMoneyDeposited((MoneyDeposited) event);
                break;
            // ... handle other events
        }
    }
    
    public AccountBalance getAccountBalance(String accountId) {
        return accountBalances.get(accountId);
    }
    
    public BigDecimal getTotalBalance() {
        return accountBalances.values().stream()
            .filter(balance -> !balance.isClosed())
            .map(AccountBalance::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### TransactionHistoryProjection

Maintains complete transaction history:

- All transactions for each account
- Transaction statistics and analytics
- Date range queries
- Transaction type filtering

## Demo Applications

### EventSourcingDemo

The main demonstration that showcases:

1. **Account Creation**: Creating bank accounts with initial balances
2. **Transactions**: Deposits, withdrawals, and transfers
3. **Projections**: Building and querying read models
4. **Event Replay**: Reconstructing state from events
5. **Concurrency Control**: Handling concurrent modifications
6. **Statistics**: Event store monitoring and statistics
7. **Temporal Queries**: Querying events by time

### EventSourcingBenefitsDemo

Focuses specifically on the benefits of Event Sourcing:

1. **Complete Audit Trail**: Every change is recorded
2. **Temporal Queries**: Query state at any point in time
3. **Event Replay**: Rebuild state from events
4. **Flexible Read Models**: Multiple projections from same events
5. **Debugging Capabilities**: Complete visibility into system behavior
6. **Compliance Features**: Regulatory and audit requirements

## Running the Demos

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Building the Project

```bash
cd event-sourcing-demo
mvn clean compile
```

### Running the Main Demo

```bash
mvn exec:java -Dexec.mainClass="com.example.eventsourcing.demo.EventSourcingDemo"
```

This launches an interactive CLI with a dynamic menu so you can create accounts and perform transactions live:

- `1` Create account
- `2` List accounts (from projection)
- `3` Deposit
- `4` Withdraw
- `5` Close account
- `6` Show projections (balances, totals, counts)
- `7` Transfer between accounts (uses a single transaction ID across both events)
- `8` List all events
- `9` Replay account from events
- `10` Show event store stats
- `11` Temporal query (events from last N seconds)
- `12` View account transactions
- `0` Exit

### Running the Benefits Demo

```bash
mvn exec:java -Dexec.mainClass="com.example.eventsourcing.demo.EventSourcingBenefitsDemo"
```

### Running Tests

```bash
mvn test
```

All tests should pass. The transaction history projection supports correlated transactions (like transfers) by grouping multiple records under the same transaction ID.

## Key Concepts Explained

### 1. Events vs State

**Traditional Approach:**
- Store current state
- Update state when changes occur
- Previous state is lost

**Event Sourcing:**
- Store events (what happened)
- Derive state from events
- Complete history is preserved

### 2. Aggregate Root

An aggregate root is the entry point to an aggregate and is responsible for:
- Maintaining business invariants
- Generating events for business operations
- Coordinating changes within the aggregate
- Ensuring consistency boundaries

### 3. Event Replay

State is reconstructed by replaying events in order:

```java
// Reconstruct account from events
List<DomainEvent> events = eventStore.getEvents(accountId).get();
BankAccount account = new BankAccount(accountId, events);
```

### 4. Projections

Projections are read models built from events:

- **Eventually Consistent**: Not immediately consistent with event store
- **Optimized for Queries**: Designed for specific use cases
- **Rebuildable**: Can be rebuilt from scratch by replaying events
- **Multiple Views**: Same events can generate multiple projections

### 5. Concurrency Control

Event Sourcing uses optimistic concurrency control:

```java
// Check expected version before appending events
eventStore.appendEvents(accountId, expectedVersion, events);
```

If the expected version doesn't match the current version, a `ConcurrencyException` is thrown.

## Best Practices

### 1. Event Design

- **Immutable**: Events should never change after creation
- **Past Tense**: Use past tense for event names (e.g., "AccountOpened")
- **Complete Data**: Include all necessary data to reconstruct state
- **Versioned**: Include version information for compatibility

### 2. Aggregate Design

- **Single Responsibility**: Each aggregate should have a single responsibility
- **Consistency Boundaries**: Keep aggregates small and focused
- **Business Invariants**: Enforce business rules before generating events
- **Event Handling**: Handle all relevant events in the aggregate

### 3. Projection Design

- **Idempotent**: Projections should be idempotent (safe to replay)
- **Eventually Consistent**: Don't expect immediate consistency
- **Optimized Queries**: Design for specific query patterns
- **Error Handling**: Handle projection failures gracefully

### 4. Event Store Design

- **Atomic Operations**: Ensure events are stored atomically
- **Concurrency Control**: Implement proper concurrency control
- **Event Ordering**: Maintain proper event sequence
- **Query Capabilities**: Support various event queries

## When to Use Event Sourcing

### Good Use Cases

- **Audit Requirements**: When complete audit trails are needed
- **Compliance**: When regulatory compliance is required
- **Complex Business Logic**: When business rules are complex
- **Temporal Queries**: When you need to query state at different times
- **Debugging**: When you need to understand what happened
- **Analytics**: When you need to analyze patterns over time

### Poor Use Cases

- **Simple CRUD**: When you only need basic CRUD operations
- **High Performance**: When you need very high performance
- **Simple Domains**: When the domain is very simple
- **Limited Resources**: When you have limited development resources

## Performance Considerations

### 1. Event Store Performance

- **Batch Operations**: Batch multiple events together
- **Indexing**: Index events for efficient queries
- **Caching**: Cache frequently accessed events
- **Partitioning**: Partition events for scalability

### 2. Projection Performance

- **Incremental Updates**: Update projections incrementally
- **Background Processing**: Process projections in background
- **Caching**: Cache projection results
- **Optimization**: Optimize for specific query patterns

### 3. Memory Usage

- **Event Compression**: Compress events for storage
- **Event Archival**: Archive old events
- **Projection Optimization**: Optimize projection memory usage
- **Garbage Collection**: Tune garbage collection for event processing

## Testing

### Unit Testing

Test individual components in isolation:

```java
@Test
public void testAccountDeposit() {
    BankAccount account = new BankAccount("John", "CHECKING", new BigDecimal("1000"), "USD");
    String transactionId = account.deposit(new BigDecimal("500"), "Salary", "John");
    
    assertThat(transactionId).isNotNull();
    assertThat(account.getBalance()).isEqualTo(new BigDecimal("1500"));
    assertThat(account.getUncommittedEvents()).hasSize(2); // AccountOpened + MoneyDeposited
}
```

### Integration Testing

Test the complete system:

```java
@Test
public void testEventSourcingFlow() throws Exception {
    // Create account
    BankAccount account = new BankAccount("John", "CHECKING", new BigDecimal("1000"), "USD");
    saveAccount(account);
    
    // Perform transaction
    account.deposit(new BigDecimal("500"), "Salary", "John");
    saveAccount(account);
    
    // Reconstruct from events
    BankAccount reconstructed = loadAccount(account.getId());
    assertThat(reconstructed.getBalance()).isEqualTo(new BigDecimal("1500"));
}
```

### Projection Testing

Test projections independently:

```java
@Test
public void testBalanceProjection() {
    AccountBalanceProjection projection = new AccountBalanceProjection("TestProjection");
    
    // Process events
    projection.processEvent(new AccountOpened(/* ... */));
    projection.processEvent(new MoneyDeposited(/* ... */));
    
    // Verify projection state
    assertThat(projection.getTotalBalance()).isEqualTo(new BigDecimal("1500"));
}
```

## Future Enhancements

### 1. Persistence

- **Database Event Store**: Implement persistent event store using database
- **Event Streaming**: Use event streaming platforms (Kafka, etc.)
- **Event Sourcing Frameworks**: Integrate with existing frameworks

### 2. Advanced Features

- **Event Versioning**: Handle event schema evolution
- **Event Compression**: Compress events for storage efficiency
- **Event Archival**: Archive old events for long-term storage
- **Event Replay**: Advanced event replay capabilities

### 3. Monitoring and Observability

- **Metrics**: Add comprehensive metrics and monitoring
- **Logging**: Enhanced logging and tracing
- **Health Checks**: Health check endpoints
- **Performance Monitoring**: Performance monitoring and alerting

### 4. Security

- **Event Encryption**: Encrypt sensitive events
- **Access Control**: Implement access control for events
- **Audit Logging**: Enhanced audit logging
- **Compliance**: Additional compliance features

## Conclusion

This Event Sourcing demo provides a comprehensive understanding of:

- **Core Concepts**: Events, aggregates, projections, and event stores
- **Implementation**: Complete working implementation with examples
- **Benefits**: Clear demonstration of Event Sourcing advantages
- **Best Practices**: Guidelines for implementing Event Sourcing
- **Use Cases**: When to use and when not to use Event Sourcing

Event Sourcing is a powerful pattern that provides significant benefits for systems that need:
- Complete audit trails
- Temporal queries
- Complex business logic
- Compliance requirements
- Debugging capabilities

While it adds complexity, the benefits often outweigh the costs for the right use cases.

## Additional Resources

- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event Store Documentation](https://eventstore.com/docs/)

## License

This project is for educational purposes and demonstrates Event Sourcing concepts and implementation.
