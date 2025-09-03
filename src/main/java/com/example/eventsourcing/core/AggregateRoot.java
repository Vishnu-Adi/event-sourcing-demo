package com.example.eventsourcing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AggregateRoot {
    
    private final String id;
    private long version;
    private final List<DomainEvent> uncommittedEvents;
    
    protected AggregateRoot(String id) {
        this.id = id;
        this.version = 0;
        this.uncommittedEvents = new ArrayList<>();
    }
    
    protected AggregateRoot(String id, List<DomainEvent> events) {
        this.id = id;
        this.version = 0;
        this.uncommittedEvents = new ArrayList<>();
        
        for (DomainEvent event : events) {
            applyEvent(event, false);
            this.version = event.getAggregateVersion();
        }
    }
    
    public String getId() {
        return id;
    }
    
    public long getVersion() {
        return version;
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    protected void applyEvent(DomainEvent event, boolean isNew) {
        handleEvent(event);
        
        if (isNew) {
            uncommittedEvents.add(event);
            this.version = event.getAggregateVersion();
        }
    }
    
    protected void applyNewEvent(DomainEvent event) {
        applyEvent(event, true);
    }
    
    protected abstract void handleEvent(DomainEvent event);
    
    protected static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', version=%d, uncommittedEvents=%d}",
                getClass().getSimpleName(), id, version, uncommittedEvents.size());
    }
}