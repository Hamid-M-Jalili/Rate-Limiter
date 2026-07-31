package com.ratelimiter.interfaces;

/**
 * Interface for token bucket rate limiting implementations
 */
public interface BucketRateLimiter extends RateLimiter {
    
    /**
     * Checks if a request from a client is allowed based on token bucket rate limits
     * @param clientId Unique identifier for the client
     * @param tokensPerSecond Number of tokens to add per second
     * @param maxBurstSize Maximum number of tokens that can be accumulated
     * @return true if request is allowed, false otherwise
     */
    boolean isAllowed(String clientId, int tokensPerSecond, int maxBurstSize);
    
    /**
     * Gets the remaining number of tokens for a client in token bucket algorithm
     * @param clientId Unique identifier for the client
     * @param tokensPerSecond Number of tokens to add per second
     * @param maxBurstSize Maximum number of tokens that can be accumulated
     * @return Number of remaining tokens
     */
    int getRemainingRequests(String clientId, int tokensPerSecond, int maxBurstSize);

    /**
     * Gets the time until next token becomes available for a client in token bucket algorithm
     * @param clientId Unique identifier for the client
     * @param tokensPerSecond Number of tokens to add per second
     * @param maxBurstSize Maximum number of tokens that can be accumulated
     * @return Time in milliseconds until next token is available
     */
    long getTimeToReset(String clientId, int tokensPerSecond, int maxBurstSize);

}