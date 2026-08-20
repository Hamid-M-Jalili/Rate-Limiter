package Demos;

import com.ratelimiter.implementations.TokenBucketRateLimiter;

public class TokenBucketRateLimiterRefillDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Token Bucket Rate Limiter Refill Behavior Test ===");
        
        // Create a rate limiter with 2 tokens per second and burst of 3
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 3);
        
        String clientId = "testClient";
        
        System.out.println("Config: 2 tokens/sec, burst capacity 3");
        System.out.println();
        
        // Test 1: Initial burst (should allow all 3)
        System.out.println("1. Testing initial burst (should allow 3 requests):");
        for (int i = 0; i < 3; i++) {
            boolean allowed = limiter.isAllowed(clientId);
            int remaining = limiter.getRemainingRequests(clientId);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d%n", 
                              i+1, allowed, remaining);
        }
        
        System.out.println();
        
        // Test 2: Try to use more tokens (should be denied)
        System.out.println("2. Testing rate limiting behavior (should deny requests):");
        for (int i = 0; i < 3; i++) {
            boolean allowed = limiter.isAllowed(clientId);
            int remaining = limiter.getRemainingRequests(clientId);
            long resetTime = limiter.getTimeToReset(clientId);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              i+1, allowed, remaining, resetTime);
        }
        
        System.out.println();
        
        // Test 3: Show that we can use all tokens and get denied
        System.out.println("3. Using up all available tokens (should deny):");
        boolean allowed = limiter.isAllowed(clientId);
        int remaining = limiter.getRemainingRequests(clientId);
        long resetTime = limiter.getTimeToReset(clientId);
        System.out.printf("   Request after burst: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          allowed, remaining, resetTime);
        
        System.out.println();
        
        // Test 4: Wait for refill and try again
        System.out.println("4. Waiting for token refill (2 seconds at 2 tokens/sec):");
        Thread.sleep(1500); // Sleep for 1.5 seconds to allow some refills
        
        System.out.println("5. After refill, trying requests:");
        for (int i = 0; i < 3; i++) {
            boolean allowedRequest = limiter.isAllowed(clientId);
            int remainingTokens = limiter.getRemainingRequests(clientId);
            long resetTimeToNextToken = limiter.getTimeToReset(clientId);
            System.out.printf("   Request %d: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                              i+1, allowedRequest, remainingTokens, resetTimeToNextToken);
        }
        
        // Test 5: Show the refill process more clearly
        System.out.println();
        System.out.println("6. Testing token refill behavior with longer wait:");
        Thread.sleep(2000); // Wait for multiple refills
        
        boolean finalAllowed = limiter.isAllowed(clientId);
        int finalRemaining = limiter.getRemainingRequests(clientId);
        long finalResetTime = limiter.getTimeToReset(clientId);
        System.out.printf("   After 2 seconds: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          finalAllowed, finalRemaining, finalResetTime);
        
        // Test 6: Show that we can make requests after refill
        System.out.println();
        System.out.println("7. Verifying refill worked:");
        boolean secondRequest = limiter.isAllowed(clientId);
        int secondRemaining = limiter.getRemainingRequests(clientId);
        long secondResetTime = limiter.getTimeToReset(clientId);
        System.out.printf("   Second request after refill: Allowed=%s, Remaining=%d, Reset in=%d ms%n", 
                          secondRequest, secondRemaining, secondResetTime);
        
        System.out.println("\n=== Test completed ===");
    }
}