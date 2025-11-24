# Resilience4j Testing Guide

This folder contains scripts and documentation for testing and demonstrating Resilience4j Retry and Circuit Breaker functionality.

## Quick Start

### Prerequisites

1. Make sure all services are running:
   ```bash
   docker compose ps
   ```

2. If services are not running, start them:
   ```bash
   docker compose up -d
   ```

### Test Scripts

#### 1. `test-resilience.sh` - Full Test Suite

Comprehensive automated test script with multiple options:

```bash
# Test Retry mechanism (3 attempts)
./test-resilience.sh retry

# Test Circuit Breaker (opens after 3 failures)
./test-resilience.sh circuit-breaker

# Monitor logs in real-time
./test-resilience.sh monitor

# Run all tests
./test-resilience.sh all
```

**What it does:**
- Stops notification service to simulate failures
- Creates users to trigger retry/circuit breaker
- Shows filtered logs with only relevant information
- Verifies expected behavior
- Restarts services after tests

#### 2. `test-resilience-simple.sh` - Simple Log Filter

Quick script to filter and show relevant logs:

```bash
# Show all Resilience4j logs (default pattern)
./test-resilience-simple.sh

# Show only Retry logs
./test-resilience-simple.sh "RETRY|🔄|❌"

# Show only Circuit Breaker logs
./test-resilience-simple.sh "Circuit breaker|OPEN|CLOSED|🚫|🔌"

# Show last 500 lines with custom pattern
./test-resilience-simple.sh "RETRY|Circuit breaker" 500
```

## Manual Testing Steps

### Test 1: Retry Mechanism

**Goal:** Verify that Retry makes 3 attempts when notification service is down.

**Steps:**

1. **Terminal 1** - Monitor logs in real-time:
   ```bash
   ./test-resilience.sh monitor
   ```

2. **Terminal 2** - Run the test:
   ```bash
   ./test-resilience.sh retry
   ```

**Expected Output:**
- `📧 Calling notification service` (appears 3 times)
- `🔄 RETRY #1` - First retry after ~500ms
- `🔄 RETRY #2` - Second retry after ~1s
- `❌ RETRY FAILED` - After all retries exhausted
- Total time: ~2-3 seconds

### Test 2: Circuit Breaker

**Goal:** Verify that Circuit Breaker opens after 3 failures and blocks subsequent calls immediately.

**Steps:**

1. **Terminal 1** - Monitor logs in real-time:
   ```bash
   ./test-resilience.sh monitor
   ```

2. **Terminal 2** - Run the test:
   ```bash
   ./test-resilience.sh circuit-breaker
   ```

**Expected Output:**

**Calls 1-3 (CB CLOSED):**
- Each call does 3 retry attempts
- `🔄 RETRY #1`, `🔄 RETRY #2` for each call
- `❌ RETRY FAILED` after all retries

**After 3 calls:**
- `🔌 Circuit breaker state transition: CLOSED -> OPEN`

**Call 4+ (CB OPEN):**
- `🚫 Circuit breaker is OPEN - Call not permitted`
- **NO retry logs** (call blocked immediately)
- Call duration < 100ms (very fast)

## Understanding the Results

### Retry Behavior

- **When service is down:** Retry makes 3 attempts with fixed 500ms interval between each
- **Total time:** ~2-3 seconds per call
- **Use case:** Handles transient errors (network issues, temporary unavailability)

### Circuit Breaker Behavior

- **When CLOSED:** Allows calls, Retry works normally
- **After 3 failures:** Opens automatically
- **When OPEN:** Blocks all calls immediately (no retry, saves resources)
- **After 30 seconds:** Transitions to HALF_OPEN to test recovery
- **Use case:** Protects from completely down services

### Combined Behavior

1. **Service unstable (CB CLOSED):** Retry handles transient errors
2. **Service down (CB OPEN):** Circuit Breaker blocks calls immediately
3. **Service recovered (CB HALF_OPEN → CLOSED):** Retry resumes working

## Troubleshooting

### Services not responding

```bash
# Check if all services are running
docker compose ps

# Check service logs
docker compose logs user-service --tail=50
```

### No logs appearing

```bash
# Check if user-service is running
docker compose ps user-service

# Check if logs are being generated
docker compose logs user-service --tail=100 | grep -E "RETRY|Circuit breaker"
```

### Circuit Breaker not opening

- Verify that notification service is stopped: `docker compose ps notification-mock`
- Check that at least 3 calls were made
- Verify Circuit Breaker configuration in `application.yml`

## Configuration

The Resilience4j configuration is in:
- `user-service/src/main/resources/application.yml`

Key settings:
- `circuit-breaker-aspect-order: 1` - Circuit Breaker executes first
- `retry-aspect-order: 2` - Retry executes after Circuit Breaker
- `ignore-exceptions: CallNotPermittedException` - Retry ignores CB OPEN state

## Additional Resources

- Full testing documentation: `TESTING.md`
- API reference: `../API_REFERENCE.md`
- Main README: `../README.md`

