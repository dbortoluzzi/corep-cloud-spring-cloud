# Quick Testing Guide

Quick reference for testing the Spring Cloud demo application.

## Automated Test Scripts

We provide test scripts in the `test/` folder for easy testing and demonstration.

**See `test/README.md` for detailed instructions and `test/DEMO_GUIDE.md` for presentation guide.**

Quick start:
```bash
cd test

# Test Retry mechanism
./test-resilience.sh retry

# Test Circuit Breaker
./test-resilience.sh circuit-breaker

# Monitor logs in real-time
./test-resilience.sh monitor

# Run all tests
./test-resilience.sh all
```

## Service URLs

- **Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081
- **Consul UI**: http://localhost:8500
- **Notification Mock**: http://localhost:9090

## Quick Tests

### 1. Health Checks

```bash
# Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health
```

### 2. Create User (via Gateway)

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

### 3. Get User by ID

```bash
curl http://localhost:8080/api/users/1
```

### 4. Get All Users

```bash
curl http://localhost:8080/api/users
```

### 5. Verify Notification Was Sent

When creating a user, a welcome notification is automatically sent. Check logs:

```bash
# View notification logs
docker compose logs user-service --tail=20 | grep -E "✅|⚠️|Notification"

# Or monitor in real-time
docker compose logs -f user-service | grep --line-buffered -E "✅|⚠️|Notification"
```

**Expected output:**
- ✅ `Notification sent successfully` - Notification worked
- ⚠️ `Notification service unavailable, using fallback` - Notification failed, user still created

### 6. Verify Consul Service Discovery

1. Open browser: http://localhost:8500
2. Click **Services** tab
3. You should see: `gateway-service` and `user-service` (both healthy)

### 7. Test Circuit Breaker

```bash
# Stop notification service
docker compose stop notification-mock

# Create user (should still work with fallback)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com"}'

# Check logs for fallback message
docker compose logs user-service --tail=10 | grep fallback

# Restart notification service
docker compose start notification-mock
```

## Test Resilience4j Retry (3 Attempts)

### Manual Test: Verify Retry Works

**Step 1**: Monitor logs in real-time (Terminal 1):
```bash
docker compose logs -f user-service | grep --line-buffered -E 'RETRY|🔄|❌|📧'
```

**Simple grep** (retry only):
```bash
docker compose logs -f user-service | grep --line-buffered RETRY
```

**Step 2**: Stop notification service and create a user (Terminal 2):
```bash
# Stop notification service
docker compose stop notification-mock

# Create a user (will trigger retries)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Retry Test", "email": "retrytest@example.com"}'
```

**What you'll see in logs:**
- `📧 Calling notification service` (appears 3 times - one per attempt)
- `🔄 RETRY #1` - First retry after ~500ms
- `🔄 RETRY #2` - Second retry after ~1s
- `❌ RETRY FAILED` - After all retries exhausted
- `⚠️ All retry attempts failed` - Fallback called

**Total time**: ~2-3 seconds (3 attempts with exponential backoff)

### Complete Retry Test

```bash
# 1. Stop notification service
docker compose stop notification-mock

# 2. Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Retry Test", "email": "retrytest@example.com"}'

# 3. Wait for retries to complete
sleep 5

# 4. Check retry logs
docker compose logs user-service --tail=100 | grep RETRY
```

## Test Circuit Breaker (Blocks calls after N failures)

### Manual Test: Verify Circuit Breaker Opens

**IMPORTANT**: Circuit Breaker opens after **3 failed calls** (50% failure rate, minimum 3 calls required).

**Step 1**: Monitor logs in real-time (Terminal 1):
```bash
docker compose logs -f user-service | grep --line-buffered -E 'Circuit breaker|OPEN|CLOSED|🚫|Call not permitted'
```

**Step 2**: Stop notification service and make 4 consecutive calls (Terminal 2):
```bash
# Stop notification service
docker compose stop notification-mock

# Make 4 consecutive calls (after 3, CB should open)
for i in {1..4}; do
  echo "Call $i:"
  curl -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"CB Test $i\", \"email\": \"cbtest$i@example.com\"}"
  sleep 2
done
```

**Step 3**: Make a 5th call (should be IMMEDIATE, no retry):
```bash
# This call should be IMMEDIATE (< 100ms) if CB is OPEN
time curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "CB Test 5", "email": "cbtest5@example.com"}'
```

**What you'll see in logs:**
- Calls 1-3: `🔄 RETRY #1`, `🔄 RETRY #2` (each call does 3 retries)
- After 3 calls: `🔌 Circuit breaker state transition: CLOSED -> OPEN`
- Call 4+: `🚫 Circuit breaker is OPEN - Call not permitted` (IMMEDIATE, no retry)

**Key difference**:
- **With Retry**: Each call takes ~2-3 seconds (3 attempts)
- **With Circuit Breaker OPEN**: Call is immediate (< 100ms, no retry)

### Complete Circuit Breaker Test

```bash
# 1. Stop notification service
docker compose stop notification-mock

# 2. Make 4 calls to open Circuit Breaker
for i in {1..4}; do
  curl -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"CB $i\", \"email\": \"cb$i@example.com\"}" > /dev/null
  sleep 2
done

# 3. Verify Circuit Breaker is OPEN
docker compose logs user-service --tail=200 | grep "Circuit breaker state transition"

# 4. Make a call (should be immediate)
time curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "CB After Open", "email": "cbafter@example.com"}'

# 5. Verify in logs that there are no retries
docker compose logs user-service --tail=50 | grep -E "cbafter|RETRY|Call not permitted"
```

## Disable Retry or Circuit Breaker

To temporarily disable Retry or Circuit Breaker, modify `application.yml`:

```yaml
resilience4j:
  circuit-breaker-enabled: false  # Disable Circuit Breaker
  retry-enabled: false            # Disable Retry
```

Then restart the service:
```bash
docker compose restart user-service
```

## Is it correct to use Retry and Circuit Breaker together?

**YES, it's correct and common!** Here's why:

1. **Retry**: Handles transient errors (network, timeout)
   - Automatically retries for temporary errors
   - Useful when service is unstable but working

2. **Circuit Breaker**: Protects from completely down services
   - After N failures, blocks calls for a period
   - Avoids wasting resources on down services
   - When OPEN, calls fallback immediately (no retry)

**Execution order**:
- Circuit Breaker (outer) → Retry (inner)
- If CB is OPEN: blocks immediately, no retry
- If CB is CLOSED: Retry makes its attempts
- If Retry fails: CB records the failure
- After N failures: CB opens

**Practical example**:
- Unstable service: Retry handles transient errors
- Service down: Circuit Breaker opens and blocks calls
- Service recovered: Circuit Breaker closes and Retry resumes

## Common Issues

### Services not responding

```bash
# Check if all services are running
docker compose ps

# Check service logs
docker compose logs gateway-service
docker compose logs user-service
```

## More Details

For complete API reference and detailed examples, see [API_REFERENCE.md](API_REFERENCE.md).
