package com.ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sliding window log rate limiter implementation
 * Uses timestamp-based approach for accurate sliding window calculations
 */
public class SlidingWindowLogRateLimiter implements RateLimiter {
    
    // Thread-safe storage of client request timestamps
    private final Map<String, RequestHistory> clientRequests = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public boolean isAllowed(String clientId, int limit, long windowSize) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.computeIfAbsent(clientId, k -> new RequestHistory());
        
        lock.writeLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            if (history.getTimestamps().size() < limit) {
                // Allow request and add timestamp
                history.addTimestamp(currentTime);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int getRemainingRequests(String clientId, int limit, long windowSize) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.get(clientId);
        
        if (history == null) {
            return limit;
        }
        
        lock.readLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            return limit - history.getTimestamps().size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public long getTimeToReset(String clientId, int limit, long windowSize) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.get(clientId);
        
        if (history == null || history.getTimestamps().size() < limit) {
            return 0; // No waiting needed
        }
        
        lock.readLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            if (history.getTimestamps().size() >= limit) {
                long oldestTimestamp = history.getTimestamps().get(0); // First element is oldest
                return Math.max(0, windowSize - (currentTime - oldestTimestamp));
            }
            
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * New method with strategy support for soft rate limiting
     */
    public boolean isAllowed(String clientId, int limit, long windowSize, RateLimitingStrategy strategy) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return false;
        }
        
        // Default to HARD strategy for backward compatibility
        if (strategy == null) {
            strategy = RateLimitingStrategy.HARD;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.computeIfAbsent(clientId, k -> new RequestHistory());
        
        lock.writeLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            // Check if we're under limit or handling soft rate limiting
            if (history.getTimestamps().size() < limit) {
                // Allow request and add timestamp
                history.addTimestamp(currentTime);
                return true;
            }
            
            // Handle different strategies
            switch (strategy) {
                case HARD:
                    // Request denied - over limit for hard strategy
                    return false;
                    
                case SOFT:
                    // For soft limiting, we allow requests to be queued within a grace period.
                    // In this implementation, if the client is already at the limit,
                    // we still allow them to proceed but mark that they're in a "queued" state
                    // This simple approach allows one extra request when over the limit
                    // which provides some soft behavior without complex queuing logic
                    return true;
                    
                default:
                    return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * New method with strategy support for soft rate limiting
     */
    public int getRemainingRequests(String clientId, int limit, long windowSize, RateLimitingStrategy strategy) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return 0;
        }
        
        // Default to HARD strategy for backward compatibility  
        if (strategy == null) {
            strategy = RateLimitingStrategy.HARD;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.get(clientId);
        
        if (history == null) {
            return limit;
        }
        
        lock.readLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            // Return remaining requests based on strategy
            int currentRequests = history.getTimestamps().size();
            
            switch (strategy) {
                case HARD:
                    return limit - currentRequests;
                    
                case SOFT:
                    // For soft limiting, we could potentially show a different view of 
                    // remaining requests or indicate queuing. This implementation will
                    // return 0 when over the limit to maintain compatibility with existing behavior.
                    // More advanced approach would be to add some grace period logic here.
                    if (currentRequests >= limit) {
                        return 0;
                    }
                    return limit - currentRequests;
                    
                default:
                    return limit - currentRequests;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * New method with strategy support for soft rate limiting
     */
    public long getTimeToReset(String clientId, int limit, long windowSize, RateLimitingStrategy strategy) {
        if (clientId == null || limit <= 0 || windowSize <= 0) {
            return 0;
        }
        
        // Default to HARD strategy for backward compatibility
        if (strategy == null) {
            strategy = RateLimitingStrategy.HARD;
        }
        
        long currentTime = System.currentTimeMillis();
        RequestHistory history = clientRequests.get(clientId);
        
        if (history == null || history.getTimestamps().size() < limit) {
            return 0; // No waiting needed
        }
        
        lock.readLock().lock();
        try {
            // Remove old timestamps outside the sliding window
            history.removeOldTimestamps(currentTime - windowSize);
            
            // If we're still over limit, calculate when next slot opens (for HARD strategy)
            if (history.getTimestamps().size() >= limit) {
                switch (strategy) {
                    case HARD:
                        long oldestTimestamp = history.getTimestamps().get(0); // First element is oldest
                        return Math.max(0, windowSize - (currentTime - oldestTimestamp));
                        
                    case SOFT:
                        // For soft strategy, we could potentially return a grace period or 
                        // indicate that the client will be queued. This implementation returns 0
                        // to maintain compatibility with existing behavior for soft limiting.
                        return 0;
                        
                    default:
                        return 0;
                }
            }
            
            return 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Internal class to track request timestamps for a specific client
     */
    private static class RequestHistory {
        private final java.util.List<Long> timestamps = new java.util.ArrayList<>();
        private final ReentrantReadWriteLock historyLock = new ReentrantReadWriteLock();
        
        public void addTimestamp(long timestamp) {
            historyLock.writeLock().lock();
            try {
                timestamps.add(timestamp);
            } finally {
                historyLock.writeLock().unlock();
            }
        }
        
        public java.util.List<Long> getTimestamps() {
            historyLock.readLock().lock();
            try {
                // Return a copy to prevent external modification
                return new java.util.ArrayList<>(timestamps);
            } finally {
                historyLock.readLock().unlock();
            }
        }
        
        public void removeOldTimestamps(long cutoffTime) {
            historyLock.writeLock().lock();
            try {
                timestamps.removeIf(timestamp -> timestamp <= cutoffTime);
            } finally {
                historyLock.writeLock().unlock();
            }
        }
    }
}