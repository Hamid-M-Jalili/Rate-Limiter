package com.ratelimiter.implementations;

import com.ratelimiter.interfaces.BucketRateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Token bucket rate limiter implementation
 * Uses the token bucket algorithm for rate limiting with configurable tokens per second and burst capacity
 */
public class TokenBucketRateLimiter implements BucketRateLimiter {
    
    // Thread-safe storage of client token buckets
    private final Map<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Configuration for the token bucket rate limiter
     */
    public static class Config {
        private final int tokensPerSecond;
        private final int maxBurstSize;
        
        public Config(int tokensPerSecond, int maxBurstSize) {
            this.tokensPerSecond = tokensPerSecond;
            this.maxBurstSize = maxBurstSize;
        }
        
        public int getTokensPerSecond() {
            return tokensPerSecond;
        }
        
        public int getMaxBurstSize() {
            return maxBurstSize;
        }
    }
    
    private final Config config;
    
    /**
     * Constructor with default configuration
     */
    public TokenBucketRateLimiter(int tokensPerSecond, int maxBurstSize) {
        this.config = new Config(tokensPerSecond, maxBurstSize);
    }
    
    /**
     * Constructor with explicit configuration
     */
    public TokenBucketRateLimiter(Config config) {
        this.config = config;
    }
    
    @Override
    public boolean isAllowed(String clientId) {
        // Default implementation: using typical token bucket parameters
        return this.isAllowed(clientId, config.getTokensPerSecond(), config.getMaxBurstSize());
    }
    
    @Override
    public int getRemainingRequests(String clientId) {
        // Default implementation: using typical token bucket parameters
        return this.getRemainingRequests(clientId, config.getTokensPerSecond(), config.getMaxBurstSize());
    }
    
    @Override
    public long getTimeToReset(String clientId) {
        // Default implementation: using typical token bucket parameters
        return this.getTimeToReset(clientId, config.getTokensPerSecond(), config.getMaxBurstSize());
    }

    @Override
    public boolean isAllowed(String clientId, int tokensPerSecond, int maxBurstSize) {
        if (clientId == null || tokensPerSecond <= 0 || maxBurstSize <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        
        TokenBucket bucket = clientBuckets.computeIfAbsent(clientId, k -> new TokenBucket(tokensPerSecond, maxBurstSize));
        
        lock.writeLock().lock();
        try {
            return bucket.tryConsume(1, currentTime);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getRemainingRequests(String clientId, int tokensPerSecond, int maxBurstSize) {
        if (clientId == null || tokensPerSecond <= 0 || maxBurstSize <= 0) {
            return 0;
        }
        
        // For token bucket, return the remaining tokens in the bucket
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) {
            return maxBurstSize;
        }
        
        lock.readLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            return Math.toIntExact(bucket.getRemainingTokens(currentTime));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long getTimeToReset(String clientId, int tokensPerSecond, int maxBurstSize) {
        if (clientId == null || tokensPerSecond <= 0 || maxBurstSize <= 0) {
            return 0;
        }
        
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) {
            // No bucket exists, so we're at max capacity
            return 0;
        }
        
        lock.readLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            return bucket.getTimeToNextToken(currentTime);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Internal class representing a token bucket for a specific client
     */
    private static class TokenBucket {
        private final int tokensPerSecond;
        private final int maxBurstSize;
        
        // Current number of tokens in the bucket
        private long tokens;
        // Last time tokens were refilled
        private long lastRefillTime;
        private final ReentrantReadWriteLock bucketLock = new ReentrantReadWriteLock();
        
        public TokenBucket(int tokensPerSecond, int maxBurstSize) {
            this.tokensPerSecond = tokensPerSecond;
            this.maxBurstSize = maxBurstSize;
            this.tokens = maxBurstSize; // Start with full capacity
            this.lastRefillTime = System.currentTimeMillis();
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
                // Refill tokens based on elapsed time
                refill(currentTime);
                
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
         * Refill the token bucket based on elapsed time
         */
        private void refill(long currentTime) {
            long elapsedTime = currentTime - lastRefillTime;
            long newTokens = (elapsedTime * tokensPerSecond) / 1000; // Convert ms to seconds
            
            if (newTokens > 0) {
                tokens = Math.min(maxBurstSize, tokens + newTokens);
                lastRefillTime = currentTime;
            }
        }
        
        /**
         * Get the remaining number of tokens
         */
        public long getRemainingTokens(long currentTime) {
            bucketLock.readLock().lock();
            try {
                // Create a temporary bucket to calculate current tokens without consuming
                TokenBucket tempBucket = new TokenBucket(tokensPerSecond, maxBurstSize);
                tempBucket.tokens = this.tokens;
                tempBucket.lastRefillTime = this.lastRefillTime;
                
                tempBucket.refill(currentTime);
                return tempBucket.tokens;
            } finally {
                bucketLock.readLock().unlock();
            }
        }
        
        /**
         * Get time until next token becomes available
         */
        public long getTimeToNextToken(long currentTime) {
            bucketLock.readLock().lock();
            try {
                // Create a temporary bucket to calculate the time without consuming
                TokenBucket tempBucket = new TokenBucket(tokensPerSecond, maxBurstSize);
                tempBucket.tokens = this.tokens;
                tempBucket.lastRefillTime = this.lastRefillTime;
                
                tempBucket.refill(currentTime);
                
                if (tempBucket.tokens >= 1) {
                    return 0; // Already have at least one token
                }
                
                // Calculate time until next token is available
                long tokensNeeded = 1;
                long timeToNextToken = (tokensNeeded * 1000) / tokensPerSecond; 
                long timeSinceLastRefill = currentTime - lastRefillTime;
                
                return Math.max(0, timeToNextToken - timeSinceLastRefill);
            } finally {
                bucketLock.readLock().unlock();
            }
        }
    }
}