package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing that a bank account has been opened.
 * 
 * This event is published when a new bank account is created in the system.
 * It contains all the necessary information to reconstruct the initial state
 * of the account.
 */
public class AccountOpened extends AbstractDomainEvent {
    
    private final String accountHolderName;
    private final String accountType;
    private final BigDecimal initialBalance;
    private final String currency;
    
    /**
     * Constructor for creating a new AccountOpened event.
     * 
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param accountHolderName The name of the account holder
     * @param accountType The type of account (e.g., "CHECKING", "SAVINGS")
     * @param initialBalance The initial balance of the account
     * @param currency The currency of the account (e.g., "USD", "EUR")
     */
    public AccountOpened(String aggregateId, long aggregateVersion, long sequenceNumber,
                        String accountHolderName, String accountType, 
                        BigDecimal initialBalance, String currency) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }
    
    /**
     * Constructor for reconstructing an AccountOpened event from storage.
     * 
     * @param eventId The unique identifier for this event
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param occurredAt When this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param accountHolderName The name of the account holder
     * @param accountType The type of account
     * @param initialBalance The initial balance of the account
     * @param currency The currency of the account
     */
    public AccountOpened(UUID eventId, String aggregateId, long aggregateVersion, 
                        Instant occurredAt, long sequenceNumber,
                        String accountHolderName, String accountType, 
                        BigDecimal initialBalance, String currency) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }
    
    /**
     * Gets the name of the account holder.
     */
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    /**
     * Gets the type of the account.
     */
    public String getAccountType() {
        return accountType;
    }
    
    /**
     * Gets the initial balance of the account.
     */
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    
    /**
     * Gets the currency of the account.
     */
    public String getCurrency() {
        return currency;
    }
    
    @Override
    public String getEventType() {
        return "AccountOpened";
    }
    
    @Override
    public String toString() {
        return String.format("AccountOpened{accountId='%s', holder='%s', type='%s', balance=%s %s, version=%d}",
                getAggregateId(), accountHolderName, accountType, initialBalance, currency, getAggregateVersion());
    }
}
