package Demos;

import com.ratelimiter.factory.RateLimiterFactory;
import com.ratelimiter.implementations.LeakingBucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;

public class LeakingBucketRateLimiterDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Leaking Bucket Rate Limiter Demo ===");
        
        // Create a rate limiter with 2 tokens per second and capacity of 3
        LeakingBucketRateLimiter.Config config = new LeakingBucketRateLimiter.Config(2, 3);
        RateLimiter limiter = new LeakingBucketRateLimiter(config);
        
        String clientId = "testClient";
        
        System.out.println("Config: 2 tokens/sec, capacity 3");
        System.out.println();
        
        // Test 1: Initial state - bucket is empty, no requests should be allowed
        System.out.println("1. Testing initial state (bucket empty):");
        boolean allowed = limiter.isAllowed(clientId);
        int remaining = limiter.getRemainingRequests(clientId);
        long resetTime = limiter.getTimeToReset(clientId);
        System.out.printf("   Request: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          allowed, remaining, resetTime);
        
        System.out.println();
        
        // Test 2: Wait for some tokens to leak and try again
        System.out.println("2. Waiting for token leakage (1 second):");
        Thread.sleep(1000); // Sleep for 1 second
        
        // After waiting 1 second, we should have leaked 2 tokens at rate of 2/sec
        allowed = limiter.isAllowed(clientId);
        remaining = limiter.getRemainingRequests(clientId);
        resetTime = limiter.getTimeToReset(clientId);
        System.out.printf("   Request: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          allowed, remaining, resetTime);
        
        System.out.println();
        
        // Test 3: Multiple requests that should work as tokens leak
        System.out.println("3. Testing multiple requests:");
        for (int i = 0; i < 5; i++) {
            boolean requestAllowed = limiter.isAllowed(clientId);
            int requestRemaining = limiter.getRemainingRequests(clientId);
            long requestResetTime = limiter.getTimeToReset(clientId);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              i+1, requestAllowed, requestRemaining, requestResetTime);
            
            // Wait a bit to let more tokens leak
            Thread.sleep(300); 
        }
        
        System.out.println();
        
        // Test 4: Show configuration through factory pattern (using the existing factory)
        System.out.println("4. Testing factory pattern integration:");
        try {
            RateLimiterFactory.Config factoryConfig = new RateLimiterFactory.Config(RateLimiterFactory.Algorithm.LEAKING_BUCKET)
                    .setLeakingBucketConfig(new LeakingBucketRateLimiter.Config(3, 5)); // 3 tokens/sec, capacity 5
            
            RateLimiter factoryLimiter = RateLimiterFactory.createRateLimiter(
                com.ratelimiter.RateLimitingStrategy.HARD, 
                factoryConfig
            );
            
            System.out.println("   Factory-created limiter: " + factoryLimiter.getClass().getSimpleName());
            
            boolean factoryAllowed = factoryLimiter.isAllowed(clientId);
            int factoryRemaining = factoryLimiter.getRemainingRequests(clientId);
            long factoryResetTime = factoryLimiter.getTimeToReset(clientId);
            System.out.printf("   Factory request: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              factoryAllowed, factoryRemaining, factoryResetTime);
        } catch (Exception e) {
            System.err.println("Factory test failed with error: " + e.getMessage());
        }
        
        System.out.println("\n=== Demo completed ===");
    }
}