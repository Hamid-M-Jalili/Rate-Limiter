package com.ratelimiter.interfaces;

/**
 * Base interface for all rate limiting implementations
 */
public interface RateLimiter {
    
    /**
     * Checks if a request from a client is allowed based on rate limits
     * @param clientId Unique identifier for the client
     * @return true if request is allowed, false otherwise
     */
    boolean isAllowed(String clientId);
    
    /**
     * Gets the remaining number of requests for a client 
     * @param clientId Unique identifier for the client
     * @return Number of remaining requests
     */
    int getRemainingRequests(String clientId);
    
    /**
     * Gets the time until reset for a client
     * @param clientId Unique identifier for the client
     * @return Time in milliseconds until reset
     */
    long getTimeToReset(String clientId);
}