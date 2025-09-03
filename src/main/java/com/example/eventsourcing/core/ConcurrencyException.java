package com.example.eventsourcing.core;

public class ConcurrencyException extends RuntimeException {
    
    private final String aggregateId;
    private final long expectedVersion;
    private final long actualVersion;
    
    public ConcurrencyException(String aggregateId, long expectedVersion, long actualVersion) {
        super(String.format("Concurrency conflict for aggregate %s: expected version %d, but actual version is %d",
                aggregateId, expectedVersion, actualVersion));
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public long getExpectedVersion() {
        return expectedVersion;
    }
    
    public long getActualVersion() {
        return actualVersion;
    }
}