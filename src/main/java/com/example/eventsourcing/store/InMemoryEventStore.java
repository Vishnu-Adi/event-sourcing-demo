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

/**
 * In-memory implementation of the EventStore interface.
 * 
 * This implementation stores all events in memory using concurrent data structures
 * to ensure thread safety. It's perfect for demonstrations, testing, and
 * scenarios where persistence is not required.
 * 
 * Key features:
 * - Thread-safe operations using concurrent data structures
 * - Optimistic concurrency control
 * - Event ordering and versioning
 * - Support for event replay and projections
 * - Comprehensive querying capabilities
 * 
 * Note: This implementation is not persistent - all data is lost when the
 * application shuts down. For production use, consider implementing a
 * persistent event store using a database.
 */
public class InMemoryEventStore implements EventStore {
    
    // Thread-safe storage for events by aggregate ID
    private final Map<String, List<StoredEvent>> eventsByAggregate;
    
    // Thread-safe storage for all events (for global queries)
    private final List<StoredEvent> allEvents;
    
    // Read-write locks for each aggregate to ensure thread safety
    private final Map<String, ReadWriteLock> aggregateLocks;
    
    // Global lock for operations that affect all events
    private final ReadWriteLock globalLock;
    
    /**
     * Internal class to store events with additional metadata.
     */
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
    
    /**
     * Constructor for creating a new in-memory event store.
     */
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
            
            // Validate all events belong to the same aggregate
            for (DomainEvent event : events) {
                if (!aggregateId.equals(event.getAggregateId())) {
                    throw new IllegalArgumentException(
                        String.format("Event %s belongs to aggregate %s, but expected %s",
                            event.getEventId(), event.getAggregateId(), aggregateId));
                }
            }
            
            // Get or create lock for this aggregate
            ReadWriteLock aggregateLock = aggregateLocks.computeIfAbsent(aggregateId, 
                k -> new ReentrantReadWriteLock());
            
            aggregateLock.writeLock().lock();
            try {
                // Check current version for optimistic concurrency control
                long currentVersion = getCurrentVersionInternal(aggregateId);
                
                if (currentVersion != expectedVersion) {
                    throw new ConcurrencyException(aggregateId, expectedVersion, currentVersion);
                }
                
                // Validate event versions are sequential
                long nextVersion = currentVersion == -1 ? 1 : currentVersion + 1;
                for (DomainEvent event : events) {
                    if (event.getAggregateVersion() != nextVersion) {
                        throw new IllegalArgumentException(
                            String.format("Event version %d does not match expected version %d",
                                event.getAggregateVersion(), nextVersion));
                    }
                    nextVersion++;
                }
                
                // Store events
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
            return -1; // Aggregate doesn't exist
        }
        
        aggregateLock.readLock().lock();
        try {
            List<StoredEvent> storedEvents = eventsByAggregate.get(aggregateId);
            if (storedEvents == null || storedEvents.isEmpty()) {
                return -1; // Aggregate doesn't exist
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
    
    /**
     * Gets statistics about the event store.
     * Useful for monitoring and debugging.
     */
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
    
    /**
     * Statistics about the event store.
     */
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
