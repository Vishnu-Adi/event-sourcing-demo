package com.example.eventsourcing.projection;

import com.example.eventsourcing.core.DomainEvent;

import java.util.List;

/**
 * Interface for event projections in an Event Sourcing system.
 * 
 * Projections are read models that are built by processing domain events.
 * They provide optimized views of the data for specific use cases and
 * are eventually consistent with the event store.
 * 
 * Key principles:
 * - Projections are built by processing events in order
 * - They are eventually consistent (not immediately consistent)
 * - They can be rebuilt from scratch by replaying all events
 * - They provide optimized read models for specific queries
 * - They can be materialized views, reports, or any other read model
 */
public interface EventProjection {
    
    /**
     * Gets the name of this projection.
     * Used for identification and logging purposes.
     */
    String getProjectionName();
    
    /**
     * Processes a single domain event to update the projection.
     * 
     * This method is called for each event in the order they occurred.
     * The projection should update its internal state based on the event.
     * 
     * @param event The domain event to process
     */
    void processEvent(DomainEvent event);
    
    /**
     * Processes a batch of domain events to update the projection.
     * 
     * This method can be more efficient than processing events one by one,
     * especially for projections that need to perform batch operations.
     * 
     * @param events The domain events to process
     */
    default void processEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            processEvent(event);
        }
    }
    
    /**
     * Resets the projection to its initial state.
     * 
     * This method is called when the projection needs to be rebuilt
     * from scratch. It should clear all internal state.
     */
    void reset();
    
    /**
     * Gets the current state of the projection.
     * 
     * The returned object should be immutable or a copy to prevent
     * external modifications to the projection's state.
     * 
     * @return The current state of the projection
     */
    Object getState();
    
    /**
     * Checks if the projection is in a valid state.
     * 
     * This method can be used for validation and debugging purposes.
     * 
     * @return true if the projection is in a valid state, false otherwise
     */
    default boolean isValid() {
        return true;
    }
}
