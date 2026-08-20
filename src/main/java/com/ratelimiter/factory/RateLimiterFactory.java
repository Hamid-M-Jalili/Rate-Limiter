package com.ratelimiter.factory;

import com.ratelimiter.RateLimitingStrategy;
import com.ratelimiter.implementations.LeakingBucketRateLimiter;
import com.ratelimiter.implementations.SlidingWindowLogRateLimiter;
import com.ratelimiter.implementations.TokenBucketRateLimiter;
import com.ratelimiter.interfaces.BucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;
import com.ratelimiter.interfaces.SlidingWindowRateLimiter;

/**
 * Factory class for creating different rate limiter implementations
 */
public class RateLimiterFactory {
    
    /**
     * Creates a rate limiter based on the specified strategy and configuration
     * @param strategy The rate limiting strategy to use
     * @param config Configuration parameters for the rate limiter
     * @return A configured RateLimiter instance
     */
    public static RateLimiter createRateLimiter(RateLimitingStrategy strategy, Config config) {
        switch (strategy) {
            case HARD:
                return createHardRateLimiter(config);
            case SOFT:
                return createSoftRateLimiter(config);
            default:
                throw new IllegalArgumentException("Unsupported rate limiting strategy: " + strategy);
        }
    }
    
    /**
     * Creates a hard rate limiter based on the configuration
     */
    private static RateLimiter createHardRateLimiter(Config config) {
        switch (config.getAlgorithm()) {
            case TOKEN_BUCKET:
                return new TokenBucketRateLimiter(config.getTokenBucketConfig());
            case SLIDING_WINDOW:
                return new SlidingWindowLogRateLimiter();
            case LEAKING_BUCKET:
                return new LeakingBucketRateLimiter(config.getLeakingBucketConfig());
            default:
                throw new IllegalArgumentException("Unsupported algorithm for hard strategy: " + config.getAlgorithm());
        }
    }
    
    /**
     * Creates a soft rate limiter based on the configuration
     */
    private static RateLimiter createSoftRateLimiter(Config config) {
        switch (config.getAlgorithm()) {
            case TOKEN_BUCKET:
                return new TokenBucketRateLimiter(config.getTokenBucketConfig());
            case SLIDING_WINDOW:
                // For sliding window with soft strategy, we use the enhanced version
                SlidingWindowLogRateLimiter limiter = new SlidingWindowLogRateLimiter();
                // Return a wrapper or use the existing soft logic in the implementation
                return limiter;
            case LEAKING_BUCKET:
                return new LeakingBucketRateLimiter(config.getLeakingBucketConfig());
            default:
                throw new IllegalArgumentException("Unsupported algorithm for soft strategy: " + config.getAlgorithm());
        }
    }
    
    /**
     * Configuration class for rate limiters
     */
    public static class Config {
        private final Algorithm algorithm;
        private TokenBucketRateLimiter.Config tokenBucketConfig;
        private LeakingBucketRateLimiter.Config leakingBucketConfig;
        private int slidingWindowLimit = 10;
        private long slidingWindowTime = 60000; // 1 minute
        
        public Config(Algorithm algorithm) {
            this.algorithm = algorithm;
        }
        
        public Algorithm getAlgorithm() {
            return algorithm;
        }
        
        public TokenBucketRateLimiter.Config getTokenBucketConfig() {
            return tokenBucketConfig;
        }
        
        public LeakingBucketRateLimiter.Config getLeakingBucketConfig() {
            return leakingBucketConfig;
        }
        
        public int getSlidingWindowLimit() {
            return slidingWindowLimit;
        }
        
        public long getSlidingWindowTime() {
            return slidingWindowTime;
        }
        
        public Config setTokenBucketConfig(TokenBucketRateLimiter.Config config) {
            this.tokenBucketConfig = config;
            return this;
        }
        
        public Config setLeakingBucketConfig(LeakingBucketRateLimiter.Config config) {
            this.leakingBucketConfig = config;
            return this;
        }
        
        public Config setSlidingWindowLimit(int limit) {
            this.slidingWindowLimit = limit;
            return this;
        }
        
        public Config setSlidingWindowTime(long time) {
            this.slidingWindowTime = time;
            return this;
        }
    }
    
    /**
     * Enum representing different rate limiting algorithms
     */
    public enum Algorithm {
        TOKEN_BUCKET,
        SLIDING_WINDOW,
        LEAKING_BUCKET
    }
}