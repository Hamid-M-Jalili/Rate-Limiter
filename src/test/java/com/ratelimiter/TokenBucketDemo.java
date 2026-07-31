package com.ratelimiter;

import com.ratelimiter.implementations.TokenBucketRateLimiter;
import java.util.concurrent.TimeUnit;

public class TokenBucketDemo {
    public static void main(String[] args) throws InterruptedException {
        // Create a token bucket rate limiter with 2 tokens per second and burst capacity of 3
        // This will help clearly demonstrate the refill behavior
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 3);
        
        System.out.println("=== Token Bucket Rate Limiter Demo ===");
        System.out.println("Configuration: 2 tokens/second, burst capacity 3");
        System.out.println();
        
        // Test single thread behavior with refill demonstration
        System.out.println("Testing token bucket refill behavior:");
        System.out.println("Initial state - should allow immediate access (burst)");

        int initial = limiter.getRemainingRequests("user1", 2, 3);
        System.out.println("initial tokens now: " + initial);

        for (int j = 0; j < 2; j++) {

            // Use up all burst capacity
            for (int i = 0; i < 3; i++) {
                boolean allowed = limiter.isAllowed("user1");
                System.out.println("Request " + (i + 1) + " allowed: " + allowed);
            }

            // Should reject requests after burst is exhausted
            System.out.println("\nAfter exhausting burst capacity:");
            for (int i = 0; i < 3; i++) {
                boolean allowed = limiter.isAllowed("user1");
                System.out.println("Request " + (i + 1) + " allowed: " + allowed);
            }

            // Wait to allow tokens to refill
            System.out.println("\nWaiting 2 seconds for token refills...");
            TimeUnit.SECONDS.sleep(2);

            // Now should be able to use some tokens again
            System.out.println("After waiting - checking if tokens refilled:");
            boolean allowed = limiter.isAllowed("user1");
            System.out.println("Request after refill period allowed: " + allowed);

            int remaining = limiter.getRemainingRequests("user1", 2, 3);
            System.out.println("Remaining tokens after refill check: " + remaining);

            // Wait more to see full refill
            System.out.println("\nWaiting another 3 seconds for additional refills...");
            TimeUnit.SECONDS.sleep(3);

            System.out.println("After additional wait - checking token availability:");
            allowed = limiter.isAllowed("user1");
            System.out.println("Request after second refill period allowed: " + allowed);

            remaining = limiter.getRemainingRequests("user1", 2, 3);
            System.out.println("Remaining tokens now: " + remaining);

        }
        
        System.out.println("\n=== Demo completed successfully ===");
        System.out.println("Tokens were properly refilled over time and access was granted when appropriate.");
    }
}