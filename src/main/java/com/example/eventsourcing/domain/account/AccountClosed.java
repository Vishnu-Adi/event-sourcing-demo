package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AccountClosed extends AbstractDomainEvent {
    
    private final BigDecimal finalBalance;
    private final String reason;
    private final String closedBy;
    private final String transferAccountId;
    
    public AccountClosed(String aggregateId, long aggregateVersion, long sequenceNumber,
                        BigDecimal finalBalance, String reason, String closedBy, String transferAccountId) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.finalBalance = finalBalance;
        this.reason = reason;
        this.closedBy = closedBy;
        this.transferAccountId = transferAccountId;
    }
    
    public AccountClosed(UUID eventId, String aggregateId, long aggregateVersion, 
                        Instant occurredAt, long sequenceNumber,
                        BigDecimal finalBalance, String reason, String closedBy, String transferAccountId) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.finalBalance = finalBalance;
        this.reason = reason;
        this.closedBy = closedBy;
        this.transferAccountId = transferAccountId;
    }
    
    public BigDecimal getFinalBalance() {
        return finalBalance;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getClosedBy() {
        return closedBy;
    }
    
    public String getTransferAccountId() {
        return transferAccountId;
    }
    
    @Override
    public String getEventType() {
        return "AccountClosed";
    }
    
    @Override
    public String toString() {
        return String.format("AccountClosed{accountId='%s', finalBalance=%s, reason='%s', closedBy='%s', transferAccountId='%s', version=%d}",
                getAggregateId(), finalBalance, reason, closedBy, transferAccountId, getAggregateVersion());
    }
}