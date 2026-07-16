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
            
            // Check if we're under limit
            if (history.getTimestamps().size() < limit) {
                // Allow request and add timestamp
                history.addTimestamp(currentTime);
                return true;
            }
            
            // Request denied - over limit
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
            
            // Return remaining requests
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
            
            // If we're still over limit, calculate when next slot opens
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