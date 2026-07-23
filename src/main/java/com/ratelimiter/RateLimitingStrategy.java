package com.ratelimiter;

/**
 * Enum representing different rate limiting strategies
 */
public enum RateLimitingStrategy {
    /**
     * Hard rate limiting - requests are immediately accepted or rejected
     */
    HARD,
    
    /**
     * Soft rate limiting - requests may be queued when over limit, 
     * with a grace period before rejection
     */
    SOFT,
    
    /**
     * Token bucket rate limiting - uses token-based algorithm for rate limiting
     */
    TOKEN_BUCKET
}
