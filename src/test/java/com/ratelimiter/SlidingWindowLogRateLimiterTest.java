package com.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowLogRateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowLogRateLimiter();
    }
    
    @Test
    void testIsAllowedWithinLimit() {
        // Test that requests within the limit are allowed
        assertTrue(rateLimiter.isAllowed("client1", 3, 1000)); // First request
        assertTrue(rateLimiter.isAllowed("client1", 3, 1000)); // Second request  
        assertTrue(rateLimiter.isAllowed("client1", 3, 1000)); // Third request
        assertTrue(rateLimiter.isAllowed("client2", 5, 2000)); // Different client
    }
    
    @Test
    void testIsAllowedOverLimit() {
        // Test that requests over the limit are denied
        assertTrue(rateLimiter.isAllowed("client1", 2, 1000)); // First request
        assertTrue(rateLimiter.isAllowed("client1", 2, 1000)); // Second request
        assertFalse(rateLimiter.isAllowed("client1", 2, 1000)); // Third request - over limit
    }
    
    @Test
    void testGetRemainingRequests() {
        assertEquals(3, rateLimiter.getRemainingRequests("client1", 3, 1000));
        
        rateLimiter.isAllowed("client1", 3, 1000);
        assertEquals(2, rateLimiter.getRemainingRequests("client1", 3, 1000));
        
        rateLimiter.isAllowed("client1", 3, 1000);
        rateLimiter.isAllowed("client1", 3, 1000);
        assertEquals(0, rateLimiter.getRemainingRequests("client1", 3, 1000));
    }
    
    @Test
    void testGetTimeToReset() {
        // Initially no waiting time needed
        assertEquals(0, rateLimiter.getTimeToReset("client1", 2, 1000));
        
        // After using all requests, there should be a reset time
        rateLimiter.isAllowed("client1", 2, 1000);
        rateLimiter.isAllowed("client1", 2, 1000);
        long timeToReset = rateLimiter.getTimeToReset("client1", 2, 1000);
        assertTrue(timeToReset > 0 && timeToReset <= 1000); // Should be within window
    }
    
    @Test
    void testSlidingWindowEffect() throws InterruptedException {
        // Test that old requests fall out of the sliding window
        rateLimiter.isAllowed("client1", 2, 500); // First request
        rateLimiter.isAllowed("client1", 2, 500); // Second request
        
        // Wait for half the window size to pass
        Thread.sleep(250);
        
        // Should still be allowed since we're within limit
        assertTrue(rateLimiter.isAllowed("client1", 2, 500));
        
        // Wait for full window to pass - this would remove old timestamps
        Thread.sleep(300);
        
        // After window has passed, should be able to make new requests again  
        assertTrue(rateLimiter.isAllowed("client1", 2, 500));
    }
    
    @Test
    void testDifferentClientsIndependent() {
        // Different clients should have independent limits
        assertTrue(rateLimiter.isAllowed("client1", 2, 1000));
        assertTrue(rateLimiter.isAllowed("client2", 2, 1000));
        assertTrue(rateLimiter.isAllowed("client1", 2, 1000));
        assertTrue(rateLimiter.isAllowed("client2", 2, 1000));
        
        // Both should be at limit now
        assertFalse(rateLimiter.isAllowed("client1", 2, 1000));
        assertFalse(rateLimiter.isAllowed("client2", 2, 1000));
    }
    
    @Test
    void testInvalidParameters() {
        // Test with invalid parameters - should return false/0 appropriately
        assertFalse(rateLimiter.isAllowed(null, 2, 1000));
        assertFalse(rateLimiter.isAllowed("client", 0, 1000));
        assertFalse(rateLimiter.isAllowed("client", 2, 0));
        
        assertEquals(0, rateLimiter.getRemainingRequests(null, 2, 1000));
        assertEquals(0, rateLimiter.getTimeToReset(null, 2, 1000));
    }
}