package com.ratelimiter.factory;

import com.ratelimiter.implementations.LeakingBucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;

public class FactoryIntegrationTest {
    public static void main(String[] args) {
        System.out.println("=== Factory Integration Test ===");
        
        try {
            // Test creating a LeakingBucketRateLimiter through the factory
            RateLimiterFactory.Config config = new RateLimiterFactory.Config(RateLimiterFactory.Algorithm.LEAKING_BUCKET)
                    .setLeakingBucketConfig(new LeakingBucketRateLimiter.Config(5, 10)); // 5 tokens/sec, capacity 10
            
            RateLimiter limiter = RateLimiterFactory.createRateLimiter(
                com.ratelimiter.RateLimitingStrategy.HARD, 
                config
            );
            
            System.out.println("SUCCESS: Factory created " + limiter.getClass().getSimpleName());
            System.out.println("Config: rate=5 tokens/sec, capacity=10");
            
            // Test basic functionality
            boolean allowed = limiter.isAllowed("testClient");
            int remaining = limiter.getRemainingRequests("testClient");
            long resetTime = limiter.getTimeToReset("testClient");
            
            System.out.printf("Test request - Allowed: %s, Remaining: %d, Reset in: %d ms%n", 
                            allowed, remaining, resetTime);
                            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Test completed ===");
    }
}