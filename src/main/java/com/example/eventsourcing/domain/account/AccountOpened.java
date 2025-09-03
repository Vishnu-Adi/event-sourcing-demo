package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountOpened extends AbstractDomainEvent {
    
    private final String accountHolderName;
    private final String accountType;
    private final BigDecimal initialBalance;
    
    
    public AccountOpened(String aggregateId, long aggregateVersion, long sequenceNumber,
                        String accountHolderName, String accountType,
                        BigDecimal initialBalance) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }
    
    public AccountOpened(UUID eventId, String aggregateId, long aggregateVersion,
                        Instant occurredAt, long sequenceNumber,
                        String accountHolderName, String accountType,
                        BigDecimal initialBalance) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public BigDecimal getInitialBalance() {
        return initialBalance;
    }
    
    
    @Override
    public String getEventType() {
        return "AccountOpened";
    }
    
    @Override
    public String toString() {
    return String.format("AccountOpened{accountId='%s', holder='%s', type='%s', balance=%s, version=%d}",
        getAggregateId(), accountHolderName, accountType, initialBalance, getAggregateVersion());
    }
}