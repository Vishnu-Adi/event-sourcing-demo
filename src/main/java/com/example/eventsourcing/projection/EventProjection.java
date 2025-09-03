package com.example.eventsourcing.projection;

import com.example.eventsourcing.core.DomainEvent;

import java.util.List;

public interface EventProjection {
    
    String getProjectionName();
    
    void processEvent(DomainEvent event);
    
    default void processEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            processEvent(event);
        }
    }
    
    void reset();
    
    Object getState();
    
    default boolean isValid() {
        return true;
    }
}