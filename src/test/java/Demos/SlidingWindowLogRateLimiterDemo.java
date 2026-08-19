package Demos;

import com.ratelimiter.implementations.SlidingWindowLogRateLimiter;

public class SlidingWindowLogRateLimiterDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Sliding Window Log Rate Limiter Demo ===");
        
        // Create a rate limiter with 5 requests per 5 seconds (5000ms window)
        SlidingWindowLogRateLimiter limiter = new SlidingWindowLogRateLimiter();
        
        String clientId = "testClient";
        
        System.out.println("Config: 5 requests per 5 seconds (5000ms window)");
        System.out.println();
        
        // Test 1: Initial burst (should allow all 5)
        System.out.println("1. Testing initial burst (should allow 5 requests):");
        for (int i = 0; i < 5; i++) {
            boolean allowed = limiter.isAllowed(clientId, 5, 5000);
            int remaining = limiter.getRemainingRequests(clientId, 5, 5000);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d%n", 
                              i+1, allowed, remaining);
            Thread.sleep(100); // Small delay between requests
        }
        
        System.out.println();
        
        // Test 2: Try to use more requests (should be denied)
        System.out.println("2. Testing rate limiting behavior (should deny requests):");
        for (int i = 0; i < 3; i++) {
            boolean allowed = limiter.isAllowed(clientId, 5, 5000);
            int remaining = limiter.getRemainingRequests(clientId, 5, 5000);
            long resetTime = limiter.getTimeToReset(clientId, 5, 5000);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              i+1, allowed, remaining, resetTime);
            Thread.sleep(100); // Small delay between requests
        }
        
        System.out.println();
        
        // Test 3: Show that we can use all requests and get denied
        System.out.println("3. Using up all available requests (should deny):");
        boolean allowed = limiter.isAllowed(clientId, 5, 5000);
        int remaining = limiter.getRemainingRequests(clientId, 5, 5000);
        long resetTime = limiter.getTimeToReset(clientId, 5, 5000);
        System.out.printf("   Request after burst: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          allowed, remaining, resetTime);
        
        System.out.println();
        
        // Test 4: Wait for window to slide and try again
        System.out.println("4. Waiting for sliding window to slide (6 seconds):");
        Thread.sleep(6000); // Sleep for 6 seconds to allow window to slide
        
        System.out.println("5. After window slides, trying requests:");
        for (int i = 0; i < 3; i++) {
            boolean allowedRequest = limiter.isAllowed(clientId, 5, 5000);
            int remainingTokens = limiter.getRemainingRequests(clientId, 5, 5000);
            long resetTimeToNextToken = limiter.getTimeToReset(clientId, 5, 5000);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              i+1, allowedRequest, remainingTokens, resetTimeToNextToken);
            Thread.sleep(100); // Small delay between requests
        }
        
        // Test 5: Show the window sliding process more clearly
        System.out.println();
        System.out.println("6. Testing window sliding behavior with longer wait:");
        Thread.sleep(4000); // Wait for another 4 seconds
        
        boolean finalAllowed = limiter.isAllowed(clientId, 5, 5000);
        int finalRemaining = limiter.getRemainingRequests(clientId, 5, 5000);
        long finalResetTime = limiter.getTimeToReset(clientId, 5, 5000);
        System.out.printf("   After 4 seconds: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          finalAllowed, finalRemaining, finalResetTime);
        
        // Test 6: Show that we can make requests after window slides
        System.out.println();
        System.out.println("7. Verifying window sliding worked:");
        boolean secondRequest = limiter.isAllowed(clientId, 5, 5000);
        int secondRemaining = limiter.getRemainingRequests(clientId, 5, 5000);
        long secondResetTime = limiter.getTimeToReset(clientId, 5, 5000);
        System.out.printf("   Second request after window slides: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          secondRequest, secondRemaining, secondResetTime);
        
        System.out.println("\n=== Demo completed ===");
    }
}