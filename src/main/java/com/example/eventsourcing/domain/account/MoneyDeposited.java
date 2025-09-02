package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing that money has been deposited into a bank account.
 * 
 * This event is published whenever a deposit transaction is successfully processed.
 * It contains all the information needed to reconstruct the account balance
 * and maintain an audit trail of all transactions.
 */
public class MoneyDeposited extends AbstractDomainEvent {
    
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String transactionId;
    private final String description;
    private final String depositedBy;
    
    /**
     * Constructor for creating a new MoneyDeposited event.
     * 
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param amount The amount that was deposited
     * @param newBalance The new balance after the deposit
     * @param transactionId Unique identifier for this transaction
     * @param description Description of the deposit
     * @param depositedBy Who or what initiated the deposit
     */
    public MoneyDeposited(String aggregateId, long aggregateVersion, long sequenceNumber,
                         BigDecimal amount, BigDecimal newBalance, String transactionId,
                         String description, String depositedBy) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.amount = amount;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
        this.description = description;
        this.depositedBy = depositedBy;
    }
    
    /**
     * Constructor for reconstructing a MoneyDeposited event from storage.
     * 
     * @param eventId The unique identifier for this event
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param occurredAt When this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param amount The amount that was deposited
     * @param newBalance The new balance after the deposit
     * @param transactionId Unique identifier for this transaction
     * @param description Description of the deposit
     * @param depositedBy Who or what initiated the deposit
     */
    public MoneyDeposited(UUID eventId, String aggregateId, long aggregateVersion, 
                         Instant occurredAt, long sequenceNumber,
                         BigDecimal amount, BigDecimal newBalance, String transactionId,
                         String description, String depositedBy) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.amount = amount;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
        this.description = description;
        this.depositedBy = depositedBy;
    }
    
    /**
     * Gets the amount that was deposited.
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Gets the new balance after the deposit.
     */
    public BigDecimal getNewBalance() {
        return newBalance;
    }
    
    /**
     * Gets the unique identifier for this transaction.
     */
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Gets the description of the deposit.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets who or what initiated the deposit.
     */
    public String getDepositedBy() {
        return depositedBy;
    }
    
    @Override
    public String getEventType() {
        return "MoneyDeposited";
    }
    
    @Override
    public String toString() {
        return String.format("MoneyDeposited{accountId='%s', amount=%s, newBalance=%s, transactionId='%s', description='%s', depositedBy='%s', version=%d}",
                getAggregateId(), amount, newBalance, transactionId, description, depositedBy, getAggregateVersion());
    }
}
