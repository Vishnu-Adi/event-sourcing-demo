package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing that money has been withdrawn from a bank account.
 * 
 * This event is published whenever a withdrawal transaction is successfully processed.
 * It contains all the information needed to reconstruct the account balance
 * and maintain an audit trail of all transactions.
 */
public class MoneyWithdrawn extends AbstractDomainEvent {
    
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String transactionId;
    private final String description;
    private final String withdrawnBy;
    
    /**
     * Constructor for creating a new MoneyWithdrawn event.
     * 
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param amount The amount that was withdrawn
     * @param newBalance The new balance after the withdrawal
     * @param transactionId Unique identifier for this transaction
     * @param description Description of the withdrawal
     * @param withdrawnBy Who or what initiated the withdrawal
     */
    public MoneyWithdrawn(String aggregateId, long aggregateVersion, long sequenceNumber,
                         BigDecimal amount, BigDecimal newBalance, String transactionId,
                         String description, String withdrawnBy) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.amount = amount;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
        this.description = description;
        this.withdrawnBy = withdrawnBy;
    }
    
    /**
     * Constructor for reconstructing a MoneyWithdrawn event from storage.
     * 
     * @param eventId The unique identifier for this event
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param occurredAt When this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param amount The amount that was withdrawn
     * @param newBalance The new balance after the withdrawal
     * @param transactionId Unique identifier for this transaction
     * @param description Description of the withdrawal
     * @param withdrawnBy Who or what initiated the withdrawal
     */
    public MoneyWithdrawn(UUID eventId, String aggregateId, long aggregateVersion, 
                         Instant occurredAt, long sequenceNumber,
                         BigDecimal amount, BigDecimal newBalance, String transactionId,
                         String description, String withdrawnBy) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.amount = amount;
        this.newBalance = newBalance;
        this.transactionId = transactionId;
        this.description = description;
        this.withdrawnBy = withdrawnBy;
    }
    
    /**
     * Gets the amount that was withdrawn.
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * Gets the new balance after the withdrawal.
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
     * Gets the description of the withdrawal.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets who or what initiated the withdrawal.
     */
    public String getWithdrawnBy() {
        return withdrawnBy;
    }
    
    @Override
    public String getEventType() {
        return "MoneyWithdrawn";
    }
    
    @Override
    public String toString() {
        return String.format("MoneyWithdrawn{accountId='%s', amount=%s, newBalance=%s, transactionId='%s', description='%s', withdrawnBy='%s', version=%d}",
                getAggregateId(), amount, newBalance, transactionId, description, withdrawnBy, getAggregateVersion());
    }
}
