package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MoneyDeposited extends AbstractDomainEvent {
    
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String transactionId;
    private final String description;
    private final String depositedBy;
    
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