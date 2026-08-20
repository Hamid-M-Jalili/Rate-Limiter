package com.ratelimiter;

import com.ratelimiter.implementations.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Create a rate limiter with 10 tokens per second and burst capacity of 20
        rateLimiter = new TokenBucketRateLimiter(10, 20);
    }

    @Test
    public void testIsAllowed_WithinRateLimit() {
        String clientId = "client1";
        
        // Should be allowed initially (full bucket)
        assertTrue(rateLimiter.isAllowed(clientId, 100, 60000));
        
        // Should still be allowed within burst capacity
        for (int i = 0; i < 19; i++) {
            assertTrue(rateLimiter.isAllowed(clientId, 100, 60000));
        }
    }

    @Test
    public void testIsAllowed_ExceedsRateLimit() {
        String clientId = "client2";
        
        // Fill up the bucket to maximum capacity
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiter.isAllowed(clientId, 100, 2000));
        }
        
        // Should deny requests when tokens are exhausted and no refill occurred
        assertFalse(rateLimiter.isAllowed(clientId, 100, 2000));
    }

    @Test
    public void testGetRemainingRequests() {
        String clientId = "client3";
        
        // Initially should have max burst size remaining
        assertEquals(20, rateLimiter.getRemainingRequests(clientId, 100, 60000));
        
        // After consuming one token
        rateLimiter.isAllowed(clientId, 100, 60000);
        assertEquals(19, rateLimiter.getRemainingRequests(clientId, 100, 60000));
    }

    @Test
    public void testGetTimeToReset() {
        String clientId = "client4";
        
        // Initially should return 0 as bucket is full
        assertEquals(0, rateLimiter.getTimeToReset(clientId, 100, 60000));
        
        // After consuming tokens, there might be a delay until next token becomes available
        rateLimiter.isAllowed(clientId, 100, 60000);
        long timeToReset = rateLimiter.getTimeToReset(clientId, 100, 60000);
        assertTrue(timeToReset >= 0); // Should be non-negative
    }

    @Test
    public void testMultipleClients() {
        String client1 = "client1";
        String client2 = "client2";
        
        // Both clients should be able to make requests independently
        assertTrue(rateLimiter.isAllowed(client1, 100, 60000));
        assertTrue(rateLimiter.isAllowed(client2, 100, 60000));
        
        // Each client's bucket is independent
        assertEquals(19, rateLimiter.getRemainingRequests(client1, 100, 60000));
        assertEquals(19, rateLimiter.getRemainingRequests(client2, 100, 60000));
    }

    @Test
    public void testRateLimitingWithTimeRefill() throws InterruptedException {
        // Create a slower rate limiter to make refill more visible
        TokenBucketRateLimiter slowRateLimiter = new TokenBucketRateLimiter(2, 5);
        
        String clientId = "slowClient";
        
        // Fill up the bucket
        for (int i = 0; i < 5; i++) {
            assertTrue(slowRateLimiter.isAllowed(clientId, 100, 60000));
        }
        
        // Should be denied now
        assertFalse(slowRateLimiter.isAllowed(clientId, 100, 60000));
        
        // Wait for refill (at least 500ms to ensure token refill)
        Thread.sleep(600);
        
        // Should be allowed again after refill
        assertTrue(slowRateLimiter.isAllowed(clientId, 100, 60000));
    }

    @Test
    public void testEdgeCases() {
        String clientId = "client";
        
        // Test with null client ID
        assertFalse(rateLimiter.isAllowed(null, 100, 60000));
        
        // Test with invalid parameters
        assertFalse(rateLimiter.isAllowed(clientId, 0, 60000));
        assertFalse(rateLimiter.isAllowed(clientId, 100, 0));
        
        // Test remaining requests with null client ID
        assertEquals(0, rateLimiter.getRemainingRequests(null, 100, 60000));
        
        // Test time to reset with null client ID
        assertEquals(0, rateLimiter.getTimeToReset(null, 100, 60000));
    }
    
    @Test
    public void testTokenBucketWithDifferentConfigurations() {
        // Test with different token rates and burst sizes
        TokenBucketRateLimiter limiter1 = new TokenBucketRateLimiter(5, 10);
        TokenBucketRateLimiter limiter2 = new TokenBucketRateLimiter(100, 50);
        
        String clientId = "testClient";
        
        // Both should allow initial burst
        assertTrue(limiter1.isAllowed(clientId, 100, 60000));
        assertTrue(limiter2.isAllowed(clientId, 100, 60000));
        
        // Test remaining requests with different configurations
        assertEquals(9, limiter1.getRemainingRequests(clientId, 100, 60000));
        assertEquals(49, limiter2.getRemainingRequests(clientId, 100, 60000));
    }
    
    @Test
    public void testTokenBucketExplicitConfig() {
        TokenBucketRateLimiter.Config config = new TokenBucketRateLimiter.Config(3, 15);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(config);
        
        String clientId = "configClient";
        
        // Should work with explicit configuration
        assertTrue(limiter.isAllowed(clientId, 100, 60000));
        assertEquals(14, limiter.getRemainingRequests(clientId, 100, 60000));
    }
    
    @Test
    public void testTokenBucketWithZeroRate() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(0, 5);
        
        String clientId = "zeroRateClient";
        
        // With zero rate, should only allow burst capacity initially
        assertTrue(limiter.isAllowed(clientId, 100, 60000));
        assertFalse(limiter.isAllowed(clientId, 100, 60000)); // No refill for zero rate
        
        assertEquals(4, limiter.getRemainingRequests(clientId, 100, 60000));
    }
    
    @Test
    public void testTokenBucketWithZeroBurst() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 0);
        
        String clientId = "zeroBurstClient";
        
        // With zero burst capacity, should only allow one token at a time with refill
        assertFalse(limiter.isAllowed(clientId, 100, 60000)); // No initial tokens
        
        // After waiting for refill (but still need to wait for first token)
        assertEquals(0, limiter.getRemainingRequests(clientId, 100, 60000));
    }
    
    @Test
    public void testConcurrentAccess() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(20, 40);
        
        // Test concurrent access by multiple threads
        Thread[] threads = new Thread[5];
        int[] allowedCount = {0};
        
        for (int i = 0; i < threads.length; i++) {
            final String clientId = "client" + i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (limiter.isAllowed(clientId, 100, 60000)) {
                        allowedCount[0]++;
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Verify we can make some requests without exceptions
        assertTrue(allowedCount[0] > 0);
    }
    
    @Test
    public void testGetRemainingRequestsWithRefill() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 10);
        
        String clientId = "refillClient";
        
        // Consume all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.isAllowed(clientId, 100, 60000));
        }
        
        assertEquals(0, limiter.getRemainingRequests(clientId, 100, 60000));
        
        // Wait for refill
        Thread.sleep(500);
        
        // Should have some tokens now
        long remaining = limiter.getRemainingRequests(clientId, 100, 60000);
        assertTrue(remaining > 0 && remaining <= 10);
    }
    
    @Test
    public void testGetTimeToResetWithRefill() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 5);
        
        String clientId = "timeClient";
        
        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed(clientId, 100, 60000));
        }
        
        // Should be denied now
        assertFalse(limiter.isAllowed(clientId, 100, 60000));
        
        long timeToReset = limiter.getTimeToReset(clientId, 100, 60000);
        assertTrue(timeToReset > 0);
        
        // After waiting for refill
        Thread.sleep(1200); // Wait for more than one token refill period
        
        // Should be able to make a request now
        assertTrue(limiter.isAllowed(clientId, 100, 60000));
    }
}
