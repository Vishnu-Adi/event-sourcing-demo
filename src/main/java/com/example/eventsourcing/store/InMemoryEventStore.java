package com.example.eventsourcing.store;

import com.example.eventsourcing.core.ConcurrencyException;
import com.example.eventsourcing.core.DomainEvent;
import com.example.eventsourcing.core.EventStore;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class InMemoryEventStore implements EventStore {
    
    private final Map<String, List<StoredEvent>> eventsByAggregate;
    private final List<StoredEvent> allEvents;
    private final Map<String, ReadWriteLock> aggregateLocks;
    private final ReadWriteLock globalLock;
    
    private static class StoredEvent {
        private final DomainEvent event;
        private final long globalSequenceNumber;
        private final Instant storedAt;
        
        public StoredEvent(DomainEvent event, long globalSequenceNumber, Instant storedAt) {
            this.event = event;
            this.globalSequenceNumber = globalSequenceNumber;
            this.storedAt = storedAt;
        }
        
        public DomainEvent getEvent() {
            return event;
        }
        
        public long getGlobalSequenceNumber() {
            return globalSequenceNumber;
        }
        
        public Instant getStoredAt() {
            return storedAt;
        }
    }
    
    public InMemoryEventStore() {
        this.eventsByAggregate = new ConcurrentHashMap<>();
        this.allEvents = Collections.synchronizedList(new ArrayList<>());
        this.aggregateLocks = new ConcurrentHashMap<>();
        this.globalLock = new ReentrantReadWriteLock();
    }
    
    @Override
    public CompletableFuture<Void> appendEvents(String aggregateId, long expectedVersion, List<DomainEvent> events) {
        return CompletableFuture.runAsync(() -> {
            if (events == null || events.isEmpty()) {
                throw new IllegalArgumentException("Events list cannot be null or empty");
            }
            
            for (DomainEvent event : events) {
                if (!aggregateId.equals(event.getAggregateId())) {
                    throw new IllegalArgumentException(
                        String.format("Event %s belongs to aggregate %s, but expected %s",
                            event.getEventId(), event.getAggregateId(), aggregateId));
                }
            }
            
            ReadWriteLock aggregateLock = aggregateLocks.computeIfAbsent(aggregateId, 
                k -> new ReentrantReadWriteLock());
            
            aggregateLock.writeLock().lock();
            try {
                long currentVersion = getCurrentVersionInternal(aggregateId);
                
                if (currentVersion != expectedVersion) {
                    throw new ConcurrencyException(aggregateId, expectedVersion, currentVersion);
                }
                
                long nextVersion = currentVersion == -1 ? 1 : currentVersion + 1;
                for (DomainEvent event : events) {
                    if (event.getAggregateVersion() != nextVersion) {
                        throw new IllegalArgumentException(
                            String.format("Event version %d does not match expected version %d",
                                event.getAggregateVersion(), nextVersion));
                    }
                    nextVersion++;
                }
                
                List<StoredEvent> aggregateEvents = eventsByAggregate.computeIfAbsent(aggregateId, 
                    k -> Collections.synchronizedList(new ArrayList<>()));
                
                globalLock.writeLock().lock();
                try {
                    long globalSequence = allEvents.size();
                    Instant storedAt = Instant.now();
                    
                    for (DomainEvent event : events) {
                        StoredEvent storedEvent = new StoredEvent(event, globalSequence++, storedAt);
                        aggregateEvents.add(storedEvent);
                        allEvents.add(storedEvent);
                    }
                } finally {
                    globalLock.writeLock().unlock();
                }
                
            } finally {
                aggregateLock.writeLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> getEvents(String aggregateId) {
        return CompletableFuture.supplyAsync(() -> {
            ReadWriteLock aggregateLock = aggregateLocks.get(aggregateId);
            if (aggregateLock == null) {
                return Collections.emptyList();
            }
            
            aggregateLock.readLock().lock();
            try {
                List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
                if (storedEvents == null) {
                    return Collections.emptyList();
                }
                
                return storedEvents.stream()
                    .map(StoredEvent::getEvent)
                    .collect(Collectors.toList());
            } finally {
                aggregateLock.readLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> getEventsFromVersion(String aggregateId, long fromVersion) {
        return CompletableFuture.supplyAsync(() -> {
            ReadWriteLock aggregateLock = aggregateLocks.get(aggregateId);
            if (aggregateLock == null) {
                return Collections.emptyList();
            }
            
            aggregateLock.readLock().lock();
            try {
                List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
                if (storedEvents == null) {
                    return Collections.emptyList();
                }
                
                return storedEvents.stream()
                    .map(StoredEvent::getEvent)
                    .filter(event -> event.getAggregateVersion() > fromVersion)
                    .collect(Collectors.toList());
            } finally {
                aggregateLock.readLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> getEventsInRange(String aggregateId, long fromVersion, long toVersion) {
        return CompletableFuture.supplyAsync(() -> {
            ReadWriteLock aggregateLock = aggregateLocks.get(aggregateId);
            if (aggregateLock == null) {
                return Collections.emptyList();
            }
            
            aggregateLock.readLock().lock();
            try {
                List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
                if (storedEvents == null) {
                    return Collections.emptyList();
                }
                
                return storedEvents.stream()
                    .map(StoredEvent::getEvent)
                    .filter(event -> event.getAggregateVersion() >= fromVersion && 
                                   event.getAggregateVersion() <= toVersion)
                    .collect(Collectors.toList());
            } finally {
                aggregateLock.readLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<Long> getCurrentVersion(String aggregateId) {
        return CompletableFuture.supplyAsync(() -> getCurrentVersionInternal(aggregateId));
    }
    
    private long getCurrentVersionInternal(String aggregateId) {
        ReadWriteLock aggregateLock = aggregateLocks.get(aggregateId);
        if (aggregateLock == null) {
            return -1;
        }
        
        aggregateLock.readLock().lock();
        try {
            List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
            if (storedEvents == null || storedEvents.isEmpty()) {
                return -1;
            }
            
            return storedEvents.get(storedEvents.size() - 1).getEvent().getAggregateVersion();
        } finally {
            aggregateLock.readLock().unlock();
        }
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> getAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            globalLock.readLock().lock();
            try {
                return allEvents.stream()
                    .map(StoredEvent::getEvent)
                    .collect(Collectors.toList());
            } finally {
                globalLock.readLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<DomainEvent>> getEventsFromTime(Instant fromTime) {
        return CompletableFuture.supplyAsync(() -> {
            globalLock.readLock().lock();
            try {
                return allEvents.stream()
                    .map(StoredEvent::getEvent)
                    .filter(event -> !event.getOccurredAt().isBefore(fromTime))
                    .collect(Collectors.toList());
            } finally {
                globalLock.readLock().unlock();
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> aggregateExists(String aggregateId) {
        return CompletableFuture.supplyAsync(() -> {
            ReadWriteLock aggregateLock = aggregateLocks.get(aggregateId);
            if (aggregateLock == null) {
                return false;
            }
            
            aggregateLock.readLock().lock();
            try {
                List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
                return storedEvents != null && !storedEvents.isEmpty();
            } finally {
                aggregateLock.readLock().unlock();
            }
        });
    }
    
    public CompletableFuture<EventStoreStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            globalLock.readLock().lock();
            try {
                int totalEvents = allEvents.size();
                int totalAggregates = eventsByAggregate.size();
                
                Map<String, Integer> eventsPerAggregate = eventsByAggregate.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                    ));
                
                return new EventStoreStats(totalEvents, totalAggregates, eventsPerAggregate);
            } finally {
                globalLock.readLock().unlock();
            }
        });
    }
    
    public static class EventStoreStats {
        private final int totalEvents;
        private final int totalAggregates;
        private final Map<String, Integer> eventsPerAggregate;
        
        public EventStoreStats(int totalEvents, int totalAggregates, Map<String, Integer> eventsPerAggregate) {
            this.totalEvents = totalEvents;
            this.totalAggregates = totalAggregates;
            this.eventsPerAggregate = new HashMap<>(eventsPerAggregate);
        }
        
        public int getTotalEvents() {
            return totalEvents;
        }
        
        public int getTotalAggregates() {
            return totalAggregates;
        }
        
        public Map<String, Integer> getEventsPerAggregate() {
            return new HashMap<>(eventsPerAggregate);
        }
        
        @Override
        public String toString() {
            return String.format("EventStoreStats{totalEvents=%d, totalAggregates=%d, eventsPerAggregate=%s}",
                    totalEvents, totalAggregates, eventsPerAggregate);
        }
    }
}