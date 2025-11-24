# Demo Guide - Resilience4j Retry and Circuit Breaker

This guide provides step-by-step instructions for demonstrating Resilience4j Retry and Circuit Breaker functionality.

## Setup

1. **Start all services:**
   ```bash
   docker compose up -d
   ```

2. **Wait for services to be ready (30 seconds):**
   ```bash
   docker compose ps
   # Wait until all services show "Up" status
   ```

3. **Verify services are healthy:**
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   ```

## Demo 1: Retry Mechanism

### Objective
Show that the system automatically retries failed calls 3 times.

### Steps

1. **Open Terminal 1** - Start log monitoring:
   ```bash
   cd test
   ./test-resilience.sh monitor
   ```

2. **Open Terminal 2** - Run the test:
   ```bash
   cd test
   ./test-resilience.sh retry
   ```

### What to Explain

- **Retry Pattern:** Automatically retries failed calls
- **3 Attempts:** Initial call + 2 retries = 3 total attempts
- **Exponential Backoff:** Wait time increases between retries (500ms, 1s)
- **Use Case:** Handles transient errors (network issues, temporary unavailability)

### Key Points to Highlight

- ✅ System is resilient to temporary failures
- ✅ Automatic retry without manual intervention
- ✅ Exponential backoff prevents overwhelming the service
- ✅ User is still created even if notification fails (graceful degradation)

## Demo 2: Circuit Breaker

### Objective
Show that Circuit Breaker opens after multiple failures and blocks subsequent calls immediately.

### Steps

1. **Open Terminal 1** - Start log monitoring:
   ```bash
   cd test
   ./test-resilience.sh monitor
   ```

2. **Open Terminal 2** - Run the test:
   ```bash
   cd test
   ./test-resilience.sh circuit-breaker
   ```

### What to Explain

**Phase 1: Circuit Breaker CLOSED (Calls 1-3)**
- Each call does 3 retry attempts
- Circuit Breaker records failures
- After 3 failed calls, Circuit Breaker opens

**Phase 2: Circuit Breaker OPEN (Call 4+)**
- Calls are blocked immediately
- No retry attempts (saves resources)
- Very fast response (< 100ms)
- Goes directly to fallback

**Phase 3: Recovery (After 30 seconds)**
- Circuit Breaker transitions to HALF_OPEN
- Allows test calls to check if service recovered
- If successful → CLOSED, if failed → OPEN again

### Key Points to Highlight

- ✅ Protects from completely down services
- ✅ Saves resources by blocking calls immediately
- ✅ Automatic recovery testing after timeout
- ✅ Prevents cascading failures

## Demo 3: Combined Behavior

### Objective
Show how Retry and Circuit Breaker work together.

### Steps

1. **Show Retry working (service unstable):**
   ```bash
   cd test
   ./test-resilience.sh retry
   ```

2. **Show Circuit Breaker opening (service down):**
   ```bash
   cd test
   ./test-resilience.sh circuit-breaker
   ```

3. **Show recovery (restart notification service):**
   ```bash
   docker compose start notification-mock
   sleep 35  # Wait for Circuit Breaker to transition to HALF_OPEN
   curl -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{"name": "Recovery Test", "email": "recovery@example.com"}'
   ```

### What to Explain

**Execution Order:**
1. Circuit Breaker checks state FIRST
2. If OPEN → blocks immediately (no retry)
3. If CLOSED → Retry makes attempts
4. If Retry fails → Circuit Breaker records failure

**Benefits:**
- Retry handles transient errors
- Circuit Breaker protects from down services
- Combined: Best of both worlds

## Visual Aids

### Log Patterns to Look For

**Retry:**
```
📧 Calling notification service
🔄 RETRY #1 - Retrying notification service call
🔄 RETRY #2 - Retrying notification service call
❌ RETRY FAILED - All retry attempts exhausted
```

**Circuit Breaker:**
```
🔌 Circuit breaker state transition: CLOSED -> OPEN
🚫 Circuit breaker is OPEN - Call not permitted
```

**Combined (CB OPEN):**
```
🚫 Circuit breaker is OPEN - Call not permitted
⚠️ All retry attempts failed - Using fallback
```

## Common Questions

**Q: Why use both Retry and Circuit Breaker?**
A: Retry handles transient errors, Circuit Breaker protects from down services. Together they provide comprehensive resilience.

**Q: What happens when Circuit Breaker is OPEN?**
A: All calls are blocked immediately, no retry attempts, very fast response (< 100ms).

**Q: How does Circuit Breaker recover?**
A: After 30 seconds, it transitions to HALF_OPEN and allows test calls. If successful, it closes. If failed, it opens again.

**Q: Can I disable Retry or Circuit Breaker?**
A: Yes, set `resilience4j.retry-enabled: false` or `resilience4j.circuit-breaker-enabled: false` in `application.yml`.

## Tips for Presenting

1. **Start with Retry:** Easier to understand, shows immediate value
2. **Then Circuit Breaker:** Shows protection from down services
3. **Finally Combined:** Shows how they work together
4. **Use real-time monitoring:** Keep Terminal 1 open to show logs as they happen
5. **Explain the timing:** Point out the difference in response times (2-3s with retry vs < 100ms when CB is OPEN)

