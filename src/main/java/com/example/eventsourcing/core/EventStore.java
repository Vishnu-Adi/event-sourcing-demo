package com.example.eventsourcing.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventStore {
    
    CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<DomainEvent> events);
    
    CompletableFuture<List<DomainEvent>> getEvents(String aggregateId);
    
    CompletableFuture<List<DomainEvent>> getEventsFromVersion(String aggregateId, long fromVersion);
    
    CompletableFuture<List<DomainEvent>> getEventsInRange(String aggregateId, long fromVersion, long toVersion);
    
    CompletableFuture<Long> getCurrentVersion(String aggregateId);
    
    CompletableFuture<List<DomainEvent>> getAllEvents();
    
    CompletableFuture<List<DomainEvent>> getEventsFromTime(java.time.Instant fromTime);
    
    CompletableFuture<Boolean> aggregateExists(String aggregateId);
}