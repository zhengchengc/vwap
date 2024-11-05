# VWAP Calculator Service

## Overview
This service calculates the Volume Weighted Average Price (VWAP) over streaming trade data for multiple currency pairs. The VWAP is calculated on an hourly window basis for each unique currency pair.

## Features
- Real-time VWAP calculation for multiple currency pairs
- Hourly time window management
- Memory-safe implementation with automatic cleanup
- Thread-safe operations
- Input validation and error handling
- RESTful API endpoints

## Technical Stack
- Java 21
- Spring Boot 3.3.5
- Maven
- JUnit 5 for testing
- Lombok for reducing boilerplate
- SLF4J for logging


## Key Components

### Trade Model
Represents a single trade with validation:
- Timestamp
- Currency pair (format: XXX/YYY)
- Price (positive value)
- Volume (positive value)

### Window Manager
Manages hourly windows for VWAP calculations:
- Thread-safe implementation
- Memory management with LRU cache
- Automatic cleanup of expired windows
- Configurable retention period

### VWAP Calculator Service
Core service for processing trades and calculating VWAP:
- Trade validation
- Window management
- VWAP calculation strategy

## API Endpoints

### Process Trades
```
# Using raw array format
curl -X POST http://localhost:8080/api/v1/vwap/trades -H "Content-Type: application/json" -d '[                                                                                                                 ✔  15:44:17 
  ["9:30 AM", "AUD/USD", "0.6905", "106,198"],
  ["9:31 AM", "USD/JPY", "142.497", "30,995"]
]'

# Using structured format
curl -X POST http://localhost:8080/api/v1/vwap/trades/structured -H "Content-Type: application/json" -d '[
  {
    "timestamp": "9:30 AM",
    "currencyPair": "AUD/USD",
    "price": 0.6905,
    "volume": 106198
  }
]'
```

### Get VWAP
```
# Using base/quote path parameters (preferred way)
curl "http://localhost:8080/api/v1/vwap/pair/AUD/USD?timestamp=9:30%20AM"

# Using encoded currency pair as query parameter
curl "http://localhost:8080/api/v1/vwap/pair?currencyPair=AUD%2FUSD&timestamp=9:30%20AM"
```

## Configuration
```properties
# JVM arguments for memory safety
-Xmx2g
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
```

## Build and Run

### Prerequisites
- JDK 21 or higher
- Maven 3.6 or higher

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Run Application
```bash
mvn spring-boot:run
```

## Memory Safety Considerations
The implementation includes several features to prevent JVM crashes:
- Bounded queues for trade processing
- LRU cache for currency pairs
- Regular cleanup of expired data
- Configurable retention periods
- Memory usage monitoring
- Thread pool management

## Testing
Comprehensive test suite including:
- Unit tests for all components
- Integration tests for service interactions
- Concurrent operation tests
- Edge case handling
- Memory management tests

## Example Usage

### Java Client
```java
Trade trade = new Trade(
    LocalDateTime.now(),
    "EUR/USD",
    1.1234,
    1000000
);
vwapCalculatorService.processTrade(trade);
VwapResult result = vwapCalculatorService.getVwap("EUR/USD", LocalDateTime.now());
```

### curl Commands
```bash
# Process trades
curl -X POST http://localhost:8080/api/v1/vwap/trades -H "Content-Type: application/json" -d '[
  ["9:30 AM", "EUR/USD", "1.1234", "1000000"]
]'

# Get VWAP
curl "http://localhost:8080/api/v1/vwap/pair/EUR/USD?timestamp=9:30%20AM"
```

## Performance Considerations
- Asynchronous trade processing
- Efficient memory usage
- Thread pool management
- Backpressure handling
- Regular cleanup operations

## Future Improvements
- Add support for more currency pairs
- Implement data persistence
- Add monitoring and metrics
- Enhanced error handling
- Performance optimizations
- API documentation with Swagger
