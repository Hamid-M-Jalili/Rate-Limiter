package com.ratelimiter;

import com.ratelimiter.implementations.LeakingBucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LeakingBucketRateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        // Create a leaking bucket with leak rate of 2 tokens per second and capacity of 5
        rateLimiter = new LeakingBucketRateLimiter(2, 5);
    }
    
    @Test
    void testIsAllowedWithinCapacity() {
        // Test that requests within the capacity are allowed
        assertTrue(rateLimiter.isAllowed("client1")); // First request
        assertTrue(rateLimiter.isAllowed("client1")); // Second request  
        assertTrue(rateLimiter.isAllowed("client1")); // Third request
        assertTrue(rateLimiter.isAllowed("client2")); // Different client
    }
    
    @Test
    void testIsAllowedOverCapacity() {
        // Test that requests over the capacity are denied initially (but may be allowed later)
        // Fill up the bucket to capacity
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.isAllowed("client1"));
        }
        
        // Should deny request when at capacity
        assertFalse(rateLimiter.isAllowed("client1")); 
    }
    
    @Test
    void testGetRemainingRequests() {
        assertEquals(5, rateLimiter.getRemainingRequests("client1"));
        
        rateLimiter.isAllowed("client1");
        assertEquals(4, rateLimiter.getRemainingRequests("client1"));
        
        // Fill up the bucket to capacity and check remaining requests
        for (int i = 0; i < 4; i++) {
            rateLimiter.isAllowed("client1");
        }
        assertEquals(0, rateLimiter.getRemainingRequests("client1"));
    }
    
    @Test
    void testGetTimeToReset() {
        // Initially no waiting time needed (bucket is empty)
        assertEquals(0, rateLimiter.getTimeToReset("client1"));
        
        // After consuming some tokens, there should be a reset time based on leak rate
        rateLimiter.isAllowed("client1");
        long timeToReset = rateLimiter.getTimeToReset("client1");
        assertTrue(timeToReset >= 0);
    }
    
    @Test
    void testLeakingEffect() throws InterruptedException {
        // Test that tokens are leaked over time, allowing new requests
        
        // Fill up the bucket to capacity
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.isAllowed("client1"));
        }
        
        // Should be denied now since bucket is full
        assertFalse(rateLimiter.isAllowed("client1"));
        
        // Wait for some time - tokens should leak out
        Thread.sleep(1200); // Sleep for 600ms (more than half a second)
        
        // After leaking, we should be able to consume again
        assertTrue(rateLimiter.isAllowed("client1"));
    }
    
    @Test
    void testDifferentClientsIndependent() {
        // Different clients should have independent limits
        assertTrue(rateLimiter.isAllowed("client2"));
        assertTrue(rateLimiter.isAllowed("client1"));
        assertTrue(rateLimiter.isAllowed("client2"));
        
        // Both should be at capacity now (5 tokens each)
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.isAllowed("client1"));
            assertTrue(rateLimiter.isAllowed("client2"));
        }
        
        // Now both should be at limit
        assertTrue(rateLimiter.isAllowed("client1"));
        assertFalse(rateLimiter.isAllowed("client2"));
    }
    
    @Test
    void testInvalidParameters() {
        // Test with invalid parameters - should return false/0 appropriately
        assertFalse(rateLimiter.isAllowed(null));
        
        assertEquals(0, rateLimiter.getRemainingRequests(null));
        assertEquals(0, rateLimiter.getTimeToReset(null));
    }
}