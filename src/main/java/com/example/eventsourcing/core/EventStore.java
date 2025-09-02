package com.example.eventsourcing.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the Event Store - the central component of Event Sourcing.
 * 
 * The Event Store is responsible for:
 * - Persisting domain events in the order they occurred
 * - Retrieving events for a specific aggregate
 * - Providing event streams for replay and projections
 * - Ensuring events are immutable once stored
 * 
 * This interface defines the contract for event storage operations.
 * Implementations can use various storage backends (in-memory, database, etc.).
 */
public interface EventStore {
    
    /**
     * Appends new events to the event store for a specific aggregate.
     * 
     * This method is atomic - either all events are stored or none are.
     * It also performs optimistic concurrency control by checking the
     * expected version of the aggregate.
     * 
     * @param aggregateId The ID of the aggregate
     * @param expectedVersion The expected current version of the aggregate
     * @param events The new events to append
     * @return A CompletableFuture that completes when events are stored
     * @throws ConcurrencyException if the expected version doesn't match the current version
     * @throws IllegalArgumentException if events list is empty or contains invalid events
     */
    CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<DomainEvent> events);
    
    /**
     * Retrieves all events for a specific aggregate.
     * 
     * Events are returned in the order they occurred (by sequence number).
     * This method is used to reconstruct the current state of an aggregate
     * by replaying all its events.
     * 
     * @param aggregateId The ID of the aggregate
     * @return A CompletableFuture containing the list of events
     */
    CompletableFuture<List<DomainEvent>> getEvents(String aggregateId);
    
    /**
     * Retrieves events for a specific aggregate from a given version.
     * 
     * This is useful for:
     * - Getting only new events since a certain point
     * - Implementing event subscriptions
     * - Optimizing replay operations
     * 
     * @param aggregateId The ID of the aggregate
     * @param fromVersion The version to start from (exclusive)
     * @return A CompletableFuture containing the list of events
     */
    CompletableFuture<List<DomainEvent>> getEventsFromVersion(String aggregateId, long fromVersion);
    
    /**
     * Retrieves events for a specific aggregate within a version range.
     * 
     * @param aggregateId The ID of the aggregate
     * @param fromVersion The starting version (inclusive)
     * @param toVersion The ending version (inclusive)
     * @return A CompletableFuture containing the list of events
     */
    CompletableFuture<List<DomainEvent>> getEventsInRange(String aggregateId, long fromVersion, long toVersion);
    
    /**
     * Gets the current version of an aggregate.
     * 
     * This is useful for optimistic concurrency control and
     * determining if an aggregate exists.
     * 
     * @param aggregateId The ID of the aggregate
     * @return A CompletableFuture containing the current version, or -1 if aggregate doesn't exist
     */
    CompletableFuture<Long> getCurrentVersion(String aggregateId);
    
    /**
     * Gets all events in the system, ordered by occurrence time.
     * 
     * This is useful for:
     * - Building read models and projections
     * - System-wide event processing
     * - Audit trails
     * 
     * @return A CompletableFuture containing all events
     */
    CompletableFuture<List<DomainEvent>> getAllEvents();
    
    /**
     * Gets all events from a specific point in time.
     * 
     * @param fromTime The starting time (inclusive)
     * @return A CompletableFuture containing events from the specified time
     */
    CompletableFuture<List<DomainEvent>> getEventsFromTime(java.time.Instant fromTime);
    
    /**
     * Checks if an aggregate exists in the event store.
     * 
     * @param aggregateId The ID of the aggregate
     * @return A CompletableFuture containing true if aggregate exists, false otherwise
     */
    CompletableFuture<Boolean> aggregateExists(String aggregateId);
}
