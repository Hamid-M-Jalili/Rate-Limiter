# Rate Limiter Library

A generic rate limiter library with two implementations: sliding window log and token bucket algorithms.

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
boolean allowed = rateLimiter.isAllowed("client1", 100, 60000); // limit and windowSize are ignored in token bucket

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
int remaining = rateLimiter.getRemainingRequests("client1", 100, 60000); // limit and windowSize are ignored

// Get time until next token becomes available
long timeToReset = rateLimiter.getTimeToReset("client1", 100, 60000); // limit and windowSize are ignored
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

#### `isAllowed(clientId, limit, windowSize)`
- **Parameters**: 
  - `clientId`: Unique identifier for the client
  - `limit`: (Ignored) Maximum number of requests allowed in the time window (rate limiting is based on tokensPerSecond)
  - `windowSize`: (Ignored) Window size in milliseconds (rate limiting is based on tokensPerSecond)  
- **Returns**: `true` if request is allowed, `false` otherwise

#### `getRemainingRequests(clientId, limit, windowSize)`
- **Parameters**: Same as `isAllowed`
- **Returns**: Number of remaining tokens for the client (not requests)

#### `getTimeToReset(clientId, limit, windowSize)`  
- **Parameters**: Same as `isAllowed`
- **Returns**: Time in milliseconds until next token becomes available

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