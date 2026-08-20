package Demos;

import com.ratelimiter.implementations.LeakingBucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;

public class SimpleLeakDemo {
    public static void main(String[] args) {
        System.out.println("=== Simple Leaky Bucket Test ===");
        
        // Create a leaking bucket with leak rate of 2 tokens per second and capacity of 5
        RateLimiter limiter = new LeakingBucketRateLimiter(2, 5);
        
        String clientId = "client1";
        
        System.out.println("Testing first request for new client:");
        boolean allowed = limiter.isAllowed(clientId);
        System.out.println("First call allowed: " + allowed);
        
        if (allowed) {
            int remaining = limiter.getRemainingRequests(clientId);
            System.out.println("Remaining requests after first call: " + remaining);
            
            long resetTime = limiter.getTimeToReset(clientId);
            System.out.println("Time to reset: " + resetTime + " ms");
        } else {
            System.out.println("ERROR: First request should be allowed but was denied!");
        }
        
        System.out.println("\n=== Test completed ===");
    }
}