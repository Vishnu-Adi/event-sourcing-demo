package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MoneyWithdrawn extends AbstractDomainEvent {
    
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String transactionId;
    private final String description;
    private final String withdrawnBy;
    
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
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public BigDecimal getNewBalance() {
        return newBalance;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getDescription() {
        return description;
    }
    
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