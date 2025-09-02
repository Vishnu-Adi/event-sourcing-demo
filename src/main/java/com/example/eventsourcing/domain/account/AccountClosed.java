package com.example.eventsourcing.domain.account;

import com.example.eventsourcing.core.AbstractDomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing that a bank account has been closed.
 * 
 * This event is published when an account is permanently closed in the system.
 * It contains information about the final state of the account and the reason
 * for closure.
 */
public class AccountClosed extends AbstractDomainEvent {
    
    private final BigDecimal finalBalance;
    private final String reason;
    private final String closedBy;
    private final String transferAccountId; // Where remaining funds were transferred
    
    /**
     * Constructor for creating a new AccountClosed event.
     * 
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param finalBalance The final balance of the account when it was closed
     * @param reason The reason for closing the account
     * @param closedBy Who or what initiated the account closure
     * @param transferAccountId The account where remaining funds were transferred (if any)
     */
    public AccountClosed(String aggregateId, long aggregateVersion, long sequenceNumber,
                        BigDecimal finalBalance, String reason, String closedBy, String transferAccountId) {
        super(aggregateId, aggregateVersion, sequenceNumber);
        this.finalBalance = finalBalance;
        this.reason = reason;
        this.closedBy = closedBy;
        this.transferAccountId = transferAccountId;
    }
    
    /**
     * Constructor for reconstructing an AccountClosed event from storage.
     * 
     * @param eventId The unique identifier for this event
     * @param aggregateId The ID of the account aggregate
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param occurredAt When this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     * @param finalBalance The final balance of the account when it was closed
     * @param reason The reason for closing the account
     * @param closedBy Who or what initiated the account closure
     * @param transferAccountId The account where remaining funds were transferred (if any)
     */
    public AccountClosed(UUID eventId, String aggregateId, long aggregateVersion, 
                        Instant occurredAt, long sequenceNumber,
                        BigDecimal finalBalance, String reason, String closedBy, String transferAccountId) {
        super(eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
        this.finalBalance = finalBalance;
        this.reason = reason;
        this.closedBy = closedBy;
        this.transferAccountId = transferAccountId;
    }
    
    /**
     * Gets the final balance of the account when it was closed.
     */
    public BigDecimal getFinalBalance() {
        return finalBalance;
    }
    
    /**
     * Gets the reason for closing the account.
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Gets who or what initiated the account closure.
     */
    public String getClosedBy() {
        return closedBy;
    }
    
    /**
     * Gets the account where remaining funds were transferred (if any).
     */
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
