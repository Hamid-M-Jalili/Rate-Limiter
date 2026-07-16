# Rate Limiter Library

A generic sliding window log rate limiter implementation in Java.

## Features

- **Sliding Window Algorithm**: Accurate rate limiting using timestamp-based calculations
- **Hard Rate Limiting**: Requests are immediately accepted or rejected 
- **Client-Level Tracking**: Each client maintains separate request history
- **Thread-Safe**: Concurrent access is properly handled
- **Memory Efficient**: Old timestamps are automatically cleaned up

## Usage

### Basic Usage

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

### Advanced Usage

```java
RateLimiter rateLimiter = new SlidingWindowLogRateLimiter();

// Get remaining requests
int remaining = rateLimiter.getRemainingRequests("client1", 10, 60000);

// Get time until reset
long timeToReset = rateLimiter.getTimeToReset("client1", 10, 60000);
```

## API Reference

### `isAllowed(clientId, limit, windowSize)`
- **Parameters**: 
  - `clientId`: Unique identifier for the client
  - `limit`: Maximum number of requests allowed in the time window
  - `windowSize`: Window size in milliseconds  
- **Returns**: `true` if request is allowed, `false` otherwise

### `getRemainingRequests(clientId, limit, windowSize)`
- **Parameters**: Same as `isAllowed`
- **Returns**: Number of remaining requests for the client

### `getTimeToReset(clientId, limit, windowSize)`  
- **Parameters**: Same as `isAllowed`
- **Returns**: Time in milliseconds until reset

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

The implementation uses a sliding window log algorithm with:
- Timestamp-based tracking of client requests
- Efficient cleanup of old timestamps  
- Thread-safe operations using ConcurrentHashMap and ReentrantReadWriteLocks
- Memory-efficient storage that automatically manages aging requests

## Requirements

- Java 11 or higher
- Maven 3.6 or higher (for building from source)

## License

MIT License