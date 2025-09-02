package com.example.eventsourcing.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for domain events that provides common functionality.
 * 
 * This class implements the DomainEvent interface and provides default
 * implementations for common event properties. Concrete event classes
 * should extend this class and only need to implement the getEventType() method.
 */
public abstract class AbstractDomainEvent implements DomainEvent {
    
    private final UUID eventId;
    private final String aggregateId;
    private final long aggregateVersion;
    private final Instant occurredAt;
    private final long sequenceNumber;
    
    /**
     * Constructor for creating a new domain event.
     * 
     * @param aggregateId The ID of the aggregate this event belongs to
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     */
    protected AbstractDomainEvent(String aggregateId, long aggregateVersion, long sequenceNumber) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredAt = Instant.now();
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * Constructor for reconstructing an event from storage.
     * Used when loading events from the event store.
     * 
     * @param eventId The unique identifier for this event
     * @param aggregateId The ID of the aggregate this event belongs to
     * @param aggregateVersion The version of the aggregate when this event occurred
     * @param occurredAt When this event occurred
     * @param sequenceNumber The sequence number within the aggregate's event stream
     */
    protected AbstractDomainEvent(UUID eventId, String aggregateId, long aggregateVersion, 
                                 Instant occurredAt, long sequenceNumber) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredAt = occurredAt;
        this.sequenceNumber = sequenceNumber;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public String getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public long getAggregateVersion() {
        return aggregateVersion;
    }
    
    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }
    
    @Override
    public String toString() {
        return String.format("%s{eventId=%s, aggregateId='%s', version=%d, occurredAt=%s, sequence=%d}",
                getEventType(), eventId, aggregateId, aggregateVersion, occurredAt, sequenceNumber);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AbstractDomainEvent that = (AbstractDomainEvent) obj;
        return eventId.equals(that.eventId);
    }
    
    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}
