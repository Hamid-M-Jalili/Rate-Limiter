package com.ratelimiter;

import com.ratelimiter.implementations.SlidingWindowLogRateLimiter;
import com.ratelimiter.implementations.TokenBucketRateLimiter;
import com.ratelimiter.interfaces.RateLimiter;
import com.ratelimiter.interfaces.SlidingWindowRateLimiter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {

    @Test
    public void testSlidingWindowRateLimiter() {
        SlidingWindowRateLimiter slidingWindow = new SlidingWindowLogRateLimiter();
        
        // Test basic functionality
        assertTrue(slidingWindow.isAllowed("client1", 5, 60000)); // Should allow first request
        
        // Test that we respect the limit 
        for (int i = 0; i < 4; i++) {
            assertTrue(slidingWindow.isAllowed("client2", 5, 60000));
        }
        
        assertFalse(slidingWindow.isAllowed("client2", 5, 60000)); // Should be over limit
        
        // Test remaining requests
        int remaining = slidingWindow.getRemainingRequests("client2", 5, 60000);
        assertEquals(0, remaining);
    }
    
    @Test
    public void testTokenBucketRateLimiter() {
        TokenBucketRateLimiter tokenBucket = new TokenBucketRateLimiter(10, 20); // 10 tokens/sec, burst of 20
        
        // Test basic functionality 
        assertTrue(tokenBucket.isAllowed("client1", 10, 20)); // Should allow first request
        
        // Test that we respect the token bucket limits
        for (int i = 0; i < 19; i++) { // Use all burst capacity
            assertTrue(tokenBucket.isAllowed("client2", 10, 20));
        }
        
        assertFalse(tokenBucket.isAllowed("client2", 10, 20)); // Should be over limit
        
        // Test remaining requests (should return number of tokens available)
        int remaining = tokenBucket.getRemainingRequests("client2", 10, 20);
        assertTrue(remaining >= 0); 
    }
    
    @Test
    public void testBaseRateLimiterInterface() {
        RateLimiter slidingWindow = new SlidingWindowLogRateLimiter();
        RateLimiter tokenBucket = new TokenBucketRateLimiter(5, 10);
        
        // Test basic functionality through base interface
        assertTrue(slidingWindow.isAllowed("client1"));
        assertTrue(tokenBucket.isAllowed("client1"));
        
        // Test remaining requests 
        int slidingRemaining = slidingWindow.getRemainingRequests("client1");
        int tokenRemaining = tokenBucket.getRemainingRequests("client1"); 
        
        assertTrue(slidingRemaining >= 0);
        assertTrue(tokenRemaining >= 0);
    }
}