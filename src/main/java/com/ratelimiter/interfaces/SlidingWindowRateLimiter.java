package com.ratelimiter.interfaces;

/**
 * Interface for sliding window rate limiting implementations
 */
public interface SlidingWindowRateLimiter extends RateLimiter {
    
    /**
     * Checks if a request from a client is allowed based on sliding window rate limits
     * @param clientId Unique identifier for the client
     * @param limit Maximum number of requests allowed in the time window
     * @param windowSize Window size in milliseconds
     * @return true if request is allowed, false otherwise
     */
    boolean isAllowed(String clientId, int limit, long windowSize);
    
    /**
     * Gets the remaining number of requests for a client in sliding window algorithm
     * @param clientId Unique identifier for the client
     * @param limit Maximum number of requests allowed in the time window
     * @param windowSize Window size in milliseconds
     * @return Number of remaining requests
     */
    int getRemainingRequests(String clientId, int limit, long windowSize);
    
    /**
     * Gets the time until reset for a client in sliding window algorithm
     * @param clientId Unique identifier for the client
     * @param limit Maximum number of requests allowed in the time window
     * @param windowSize Window size in milliseconds
     * @return Time in milliseconds until reset
     */
    long getTimeToReset(String clientId, int limit, long windowSize);
}