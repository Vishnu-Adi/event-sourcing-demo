package com.example.eventsourcing.core;

/**
 * Exception thrown when there's a concurrency conflict in the event store.
 * 
 * This happens when trying to append events to an aggregate that has been
 * modified by another process since the last read. This is a key aspect of
 * optimistic concurrency control in Event Sourcing.
 * 
 * The application should handle this exception by:
 * 1. Re-reading the current state of the aggregate
 * 2. Reapplying any business logic
 * 3. Retrying the operation with the updated expected version
 */
public class ConcurrencyException extends RuntimeException {
    
    private final String aggregateId;
    private final long expectedVersion;
    private final long actualVersion;
    
    /**
     * Creates a new ConcurrencyException.
     * 
     * @param aggregateId The ID of the aggregate that had the conflict
     * @param expectedVersion The version that was expected
     * @param actualVersion The actual current version
     */
    public ConcurrencyException(String aggregateId, long expectedVersion, long actualVersion) {
        super(String.format("Concurrency conflict for aggregate %s: expected version %d, but actual version is %d",
                aggregateId, expectedVersion, actualVersion));
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    /**
     * Gets the aggregate ID that had the concurrency conflict.
     */
    public String getAggregateId() {
        return aggregateId;
    }
    
    /**
     * Gets the expected version that was used in the operation.
     */
    public long getExpectedVersion() {
        return expectedVersion;
    }
    
    /**
     * Gets the actual current version of the aggregate.
     */
    public long getActualVersion() {
        return actualVersion;
    }
}
