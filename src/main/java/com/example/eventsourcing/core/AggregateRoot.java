package com.example.eventsourcing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abstract base class for aggregate roots in an Event Sourcing system.
 * 
 * An aggregate root is the entry point to an aggregate and is responsible for:
 * - Maintaining the current state by applying events
 * - Generating new events when business operations are performed
 * - Ensuring business invariants are maintained
 * - Coordinating changes within the aggregate
 * 
 * Key principles:
 * - State is derived from events, not stored directly
 * - Business operations generate events, not state changes
 * - Events are applied to reconstruct state
 * - Uncommitted events are tracked until persisted
 */
public abstract class AggregateRoot {
    
    private final String id;
    private long version;
    private final List<DomainEvent> uncommittedEvents;
    
    /**
     * Constructor for creating a new aggregate root.
     * 
     * @param id The unique identifier for this aggregate
     */
    protected AggregateRoot(String id) {
        this.id = id;
        this.version = 0;
        this.uncommittedEvents = new ArrayList<>();
    }
    
    /**
     * Constructor for reconstructing an aggregate from events.
     * 
     * @param id The unique identifier for this aggregate
     * @param events The events to replay to reconstruct state
     */
    protected AggregateRoot(String id, List<DomainEvent> events) {
        this.id = id;
        this.version = 0;
        this.uncommittedEvents = new ArrayList<>();
        
        // Replay all events to reconstruct the current state
        for (DomainEvent event : events) {
            applyEvent(event, false);
            this.version = event.getAggregateVersion();
        }
    }
    
    /**
     * Gets the unique identifier of this aggregate.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the current version of this aggregate.
     * This represents the number of events that have been applied.
     */
    public long getVersion() {
        return version;
    }
    
    /**
     * Gets the list of uncommitted events.
     * These events will be persisted when the aggregate is saved.
     */
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    /**
     * Clears the list of uncommitted events.
     * This is called after events have been successfully persisted.
     */
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    /**
     * Applies a domain event to this aggregate.
     * 
     * This method handles both:
     * - Replaying historical events (when reconstructing state)
     * - Applying new events (when performing business operations)
     * 
     * @param event The event to apply
     * @param isNew Whether this is a new event (true) or a replayed event (false)
     */
    protected void applyEvent(DomainEvent event, boolean isNew) {
        // Apply the event to update the aggregate's state
        handleEvent(event);
        
        // If this is a new event, add it to uncommitted events
        if (isNew) {
            uncommittedEvents.add(event);
            this.version = event.getAggregateVersion();
        }
    }
    
    /**
     * Applies a new domain event to this aggregate.
     * 
     * This method should be called by business methods to record
     * that something has happened in the domain.
     * 
     * @param event The new event to apply
     */
    protected void applyNewEvent(DomainEvent event) {
        applyEvent(event, true);
    }
    
    /**
     * Abstract method that concrete aggregate roots must implement
     * to handle specific domain events.
     * 
     * This method is called whenever an event is applied to the aggregate,
     * whether it's a new event or a replayed event.
     * 
     * @param event The event to handle
     */
    protected abstract void handleEvent(DomainEvent event);
    
    /**
     * Generates a new unique identifier.
     * Utility method for creating new aggregate IDs.
     */
    protected static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', version=%d, uncommittedEvents=%d}",
                getClass().getSimpleName(), id, version, uncommittedEvents.size());
    }
}
