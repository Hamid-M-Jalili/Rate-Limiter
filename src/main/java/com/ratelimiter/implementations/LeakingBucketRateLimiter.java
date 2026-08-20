package com.ratelimiter.implementations;

import com.ratelimiter.interfaces.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Leaking bucket rate limiter implementation
 * Uses the leaking bucket algorithm for rate limiting with configurable leak rate and capacity
 */
public class LeakingBucketRateLimiter implements RateLimiter {
    
    // Thread-safe storage of client buckets
    private final Map<String, LeakingBucket> clientBuckets = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Configuration for the leaking bucket rate limiter
     */
    public static class Config {
        private final int leakRate; // tokens to leak per second
        private final int capacity; // maximum number of tokens that can be stored
        
        public Config(int leakRate, int capacity) {
            this.leakRate = leakRate;
            this.capacity = capacity;
        }
        
        public int getLeakRate() {
            return leakRate;
        }
        
        public int getCapacity() {
            return capacity;
        }
    }
    
    private final Config config;
    
    /**
     * Constructor with default configuration
     */
    public LeakingBucketRateLimiter(int leakRate, int capacity) {
        this.config = new Config(leakRate, capacity);
    }
    
    /**
     * Constructor with explicit configuration
     */
    public LeakingBucketRateLimiter(Config config) {
        this.config = config;
    }
    
    @Override
    public boolean isAllowed(String clientId) {
        if (clientId == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        LeakingBucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new LeakingBucket(config.getLeakRate(), config.getCapacity()));
        
        lock.writeLock().lock();
        try {
            return bucket.tryConsume(1, currentTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int getRemainingRequests(String clientId) {
        if (clientId == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        LeakingBucket bucket = clientBuckets.get(clientId);
        
        if (bucket == null) {
            return config.getCapacity();
        }
        
        lock.readLock().lock();
        try {
            return Math.toIntExact(bucket.getRemainingTokens(currentTime));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public long getTimeToReset(String clientId) {
        if (clientId == null) {
            return 0;
        }
        
        LeakingBucket bucket = clientBuckets.get(clientId);
        
        if (bucket == null) {
            // No bucket exists, so we're at max capacity
            return 0;
        }
        
        lock.readLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            return bucket.getTimeToNextLeak(currentTime);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Internal class representing a leaking bucket for a specific client
     */
    private static class LeakingBucket {
        private final int leakRate;
        private final int capacity;
        
        // Current number of tokens in the bucket
        private long tokens;
        // Last time tokens were leaked
        private long lastLeakTime;
        private final ReentrantReadWriteLock bucketLock = new ReentrantReadWriteLock();
        
        public LeakingBucket(int leakRate, int capacity) {
            this.leakRate = leakRate;
            this.capacity = capacity;
            this.tokens = capacity; // Start with full capacity like TokenBucket
            this.lastLeakTime = System.currentTimeMillis();
        }
        
        /**
         * Try to consume a specified number of tokens
         * @param tokenCount Number of tokens to consume
         * @param currentTime Current time in milliseconds
         * @return true if enough tokens were available, false otherwise
         */
        public boolean tryConsume(int tokenCount, long currentTime) {
            bucketLock.writeLock().lock();
            try {
                // Leak tokens based on elapsed time
                leak(currentTime);
                
                if (tokens >= tokenCount) {
                    tokens -= tokenCount;
                    return true;
                }
                return false;
            } finally {
                bucketLock.writeLock().unlock();
            }
        }
        
        /**
         * Leak tokens from the bucket based on elapsed time
         */
        private void leak(long currentTime) {
            long elapsedTime = currentTime - lastLeakTime;
            long leakedTokens = (elapsedTime * leakRate) / 1000; // Convert ms to seconds
            
            if (leakedTokens > 0) {
                tokens = Math.max(0, tokens - leakedTokens);
                lastLeakTime = currentTime;
            }
        }
        
        /**
         * Get the remaining number of tokens
         */
        public long getRemainingTokens(long currentTime) {
            bucketLock.readLock().lock();
            try {
                // Create a temporary bucket to calculate current tokens without consuming
                LeakingBucket tempBucket = new LeakingBucket(leakRate, capacity);
                tempBucket.tokens = this.tokens;
                tempBucket.lastLeakTime = this.lastLeakTime;
                
                tempBucket.leak(currentTime);
                return Math.max(0, tempBucket.tokens);
            } finally {
                bucketLock.readLock().unlock();
            }
        }
        
        /**
         * Get time until next token can be consumed (when the bucket has capacity to accept new tokens)
         */
        public long getTimeToNextLeak(long currentTime) {
            bucketLock.readLock().lock();
            try {
                // Create a temporary bucket to calculate the time without consuming
                LeakingBucket tempBucket = new LeakingBucket(leakRate, capacity);
                tempBucket.tokens = this.tokens;
                tempBucket.lastLeakTime = this.lastLeakTime;
                
                tempBucket.leak(currentTime);
                
                if (tempBucket.tokens < capacity) {
                    return 0; // Already have space to add tokens
                }
                
                // Calculate time until next leak would free up space
                long tokensNeeded = 1;
                long timeToNextLeak = (tokensNeeded * 1000) / leakRate; 
                long timeSinceLastLeak = currentTime - lastLeakTime;
                
                return Math.max(0, timeToNextLeak - timeSinceLastLeak);
            } finally {
                bucketLock.readLock().unlock();
            }
        }
    }
}