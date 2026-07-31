# Rate Limiter Library

A generic rate limiter library with two implementations: **sliding window log** and **token bucket** algorithms.

## Features

- **Sliding Window Algorithm**: Accurate rate limiting using timestamp-based calculations
- **Token Bucket Algorithm**: Rate limiting with configurable tokens per second and burst capacity  
- **Hard Rate Limiting**: Requests are immediately accepted or rejected 
- **Client-Level Tracking**: Each client maintains separate request history
- **Thread-Safe**: Concurrent access is properly handled
- **Memory Efficient**: Old timestamps are automatically cleaned up

## Usage

### Basic Usage - Sliding Window Log

```java
RateLimiter rateLimiter = new SlidingWindowLogRateLimiter();

// Check if a request is allowed (limit 10 requests per minute)
boolean allowed = rateLimiter.isAllowed("client1", 10, 60000); // 60000ms = 1 minute

if (allowed) {
    // Process the request
} else {
    // Reject the request - over limit
    System.out.println("Rate limit exceeded for client");
}
```

### Basic Usage - Token Bucket

```java
// Create a rate limiter with 10 tokens per second and burst capacity of 20
RateLimiter rateLimiter = new TokenBucketRateLimiter(10, 20);

// Check if a request is allowed 
boolean allowed = rateLimiter.isAllowed("client1", 100, 60000); 

if (allowed) {
    // Process the request
} else {
    // Reject the request - over limit
    System.out.println("Rate limit exceeded for client");
}
```

### Advanced Usage - Sliding Window Log

```java
RateLimiter rateLimiter = new SlidingWindowLogRateLimiter();

// Get remaining requests
int remaining = rateLimiter.getRemainingRequests("client1", 10, 60000);

// Get time until reset
long timeToReset = rateLimiter.getTimeToReset("client1", 10, 60000);
```

### Advanced Usage - Token Bucket

```java
// Create a rate limiter with 5 tokens per second and burst capacity of 10
RateLimiter rateLimiter = new TokenBucketRateLimiter(5, 10);

// Get remaining requests (tokens in the bucket)
int remaining = rateLimiter.getRemainingRequests("client1", 100, 60000); 

// Get time until next token becomes available
long timeToReset = rateLimiter.getTimeToReset("client1", 100, 60000);
```

## API Reference

### Sliding Window Log Algorithm

#### `isAllowed(clientId, limit, windowSize)`
- **Parameters**: 
  - `clientId`: Unique identifier for the client
  - `limit`: Maximum number of requests allowed in the time window
  - `windowSize`: Window size in milliseconds  
- **Returns**: `true` if request is allowed, `false` otherwise

#### `getRemainingRequests(clientId, limit, windowSize)`
- **Parameters**: Same as `isAllowed`
- **Returns**: Number of remaining requests for the client

#### `getTimeToReset(clientId, limit, windowSize)`  
- **Parameters**: Same as `isAllowed`
- **Returns**: Time in milliseconds until reset

### Token Bucket Algorithm

#### `isAllowed(clientId, tokensPerSecond, maxBurstSize)`
- **Parameters**: 
  - `clientId`: Unique identifier for the client
  - `tokensPerSecond`: Number of tokens to add per second (rate limiting)
  - `maxBurstSize`: Maximum number of tokens that can be accumulated (burst capacity)  
- **Returns**: `true` if request is allowed, `false` otherwise

#### `getRemainingRequests(clientId, tokensPerSecond, maxBurstSize)`
- **Parameters**: Same as `isAllowed`
- **Returns**: Number of remaining tokens for the client (not requests)

#### `getTimeToReset(clientId, tokensPerSecond, maxBurstSize)`  
- **Parameters**: Same as `isAllowed`
- **Returns**: Time in milliseconds until next token becomes available

## Backward Compatibility
For backward compatibility with older versions that used the generic RateLimiter interface:
```java
// Sliding Window (old method calls still work)
RateLimiter slidingWindow = new SlidingWindowLogRateLimiter();
boolean allowed1 = slidingWindow.isAllowed("client1"); // Uses default 10 requests per minute

// Token Bucket (old method calls still work) 
RateLimiter tokenBucket = new TokenBucketRateLimiter(10, 20);
boolean allowed2 = tokenBucket.isAllowed("client1"); // Uses default 10 tokens/sec, burst of 20
```

## Using Different Rate Limiting Strategies

The rate limiter interface supports different strategies through the `RateLimitingStrategy` parameter. This allows you to configure how requests are handled when they exceed limits:

```java
// Use hard rate limiting (immediate rejection)
boolean allowed = rateLimiter.isAllowed("client1", 10, 60000, RateLimitingStrategy.HARD);

// Use soft rate limiting (may queue or delay when over limit) 
boolean allowed = rateLimiter.isAllowed("client1", 10, 60000, RateLimitingStrategy.SOFT);

// For TokenBucket, the strategy parameter is ignored as token bucket only supports HARD strategy
boolean allowed = rateLimiter.isAllowed("client1", 10, 60000, RateLimitingStrategy.TOKEN_BUCKET);
```

The supported strategies are:
- **HARD**: Requests are immediately accepted or rejected based on limits
- **SOFT**: Requests may be queued when over limit with a grace period before rejection  
- **TOKEN_BUCKET**: Uses the token bucket algorithm for rate limiting (only available in TokenBucketRateLimiter)

## Integration

This library can be integrated into any Java project using Maven:

```xml
<dependency>
    <groupId>com.ratelimiter</groupId>
    <artifactId>rate-limiter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Or by building a JAR file and including it in your classpath.

## Implementation Details

The library provides two rate limiting algorithms:

### Sliding Window Log Algorithm
Uses timestamp-based tracking of client requests with:
- Timestamp-based tracking of client requests
- Efficient cleanup of old timestamps  
- Thread-safe operations using ConcurrentHashMap and ReentrantReadWriteLocks
- Memory-efficient storage that automatically manages aging requests

### Token Bucket Algorithm
Uses a token bucket approach with:
- Configurable tokens per second (rate limiting)
- Configurable burst capacity (maximum initial tokens) 
- Dynamic token refill based on time elapsed
- Thread-safe operations using ConcurrentHashMap and ReentrantReadWriteLocks for individual buckets
- Memory-efficient storage that automatically manages bucket state

The TokenBucketRateLimiter has two constructors:
1. `TokenBucketRateLimiter(int tokensPerSecond, int maxBurstSize)` 
2. `TokenBucketRateLimiter(Config config)`

## Requirements

- Java 11 or higher
- Maven 3.6 or higher (for building from source)

## License

MIT License