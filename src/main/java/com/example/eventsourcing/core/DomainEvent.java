package com.example.eventsourcing.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the Event Sourcing system.
 * 
 * Domain events represent something that has happened in the business domain.
 * They are immutable and contain all the data needed to reconstruct the state
 * of an aggregate at any point in time.
 * 
 * Key principles:
 * - Events are immutable (cannot be changed after creation)
 * - Events represent past occurrences (use past tense in naming)
 * - Events contain all necessary data to reconstruct state
 * - Events are the single source of truth for what happened
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this specific event instance.
     * Used for deduplication and event ordering.
     */
    UUID getEventId();
    
    /**
     * The aggregate root ID that this event belongs to.
     * Used to group events by aggregate.
     */
    String getAggregateId();
    
    /**
     * The version of the aggregate when this event occurred.
     * Used for optimistic concurrency control.
     */
    long getAggregateVersion();
    
    /**
     * When this event occurred in the system.
     * Used for temporal ordering and auditing.
     */
    Instant getOccurredAt();
    
    /**
     * The type of this event.
     * Used for event routing and deserialization.
     */
    String getEventType();
    
    /**
     * The sequence number of this event within the aggregate's event stream.
     * Used for ordering events within an aggregate.
     */
    long getSequenceNumber();
}
