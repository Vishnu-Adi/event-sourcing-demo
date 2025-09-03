package com.example.eventsourcing.core;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    
    UUID getEventId();
    
    String getAggregateId();
    
    long getAggregateVersion();
    
    Instant getOccurredAt();
    
    String getEventType();
    
    long getSequenceNumber();
}