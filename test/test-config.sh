#!/bin/bash

# Test script to verify Resilience4j configuration
# Tests both Retry and Circuit Breaker functionality

GATEWAY_URL="http://localhost:8080"
SERVICE_NAME="user-service"

echo "=========================================="
echo "Resilience4j Configuration Test"
echo "=========================================="
echo ""

# Check if services are running
echo "1. Checking services..."
if ! docker compose ps | grep -q "$SERVICE_NAME.*Up"; then
    echo "❌ $SERVICE_NAME is not running. Start it with: docker compose up -d"
    exit 1
fi
echo "✅ Services are running"
echo ""

# Test Retry
echo "2. Testing Retry (3 attempts)..."
docker compose stop notification-mock > /dev/null 2>&1
sleep 2

EMAIL="retrytest-$(date +%s)@example.com"
echo "   Creating user: $EMAIL"
curl -s -X POST "$GATEWAY_URL/api/users" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"Retry Test\", \"email\": \"$EMAIL\"}" > /dev/null

sleep 5

RETRY_COUNT=$(docker compose logs "$SERVICE_NAME" --tail=200 | grep -E "RETRY|🔄|❌" | grep "$EMAIL" | wc -l | tr -d ' ')
if [ -z "$RETRY_COUNT" ]; then
    RETRY_COUNT=0
fi
if [ "$RETRY_COUNT" -gt 0 ]; then
    echo "   ✅ Retry is working (found $RETRY_COUNT retry logs)"
else
    echo "   ❌ Retry is NOT working (no retry logs found)"
fi
echo ""

# Test Circuit Breaker
echo "3. Testing Circuit Breaker (opens after 3 failures)..."
docker compose stop notification-mock > /dev/null 2>&1
sleep 2

echo "   Making 3 calls to open Circuit Breaker..."
for i in {1..3}; do
    EMAIL="cbtest$i-$(date +%s)@example.com"
    curl -s -X POST "$GATEWAY_URL/api/users" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"CB Test $i\", \"email\": \"$EMAIL\"}" > /dev/null
    sleep 2
done

sleep 3

CB_OPEN_COUNT=$(docker compose logs "$SERVICE_NAME" --tail=500 | grep "Circuit breaker state transition.*CLOSED -> OPEN" | wc -l | tr -d ' ')
if [ -z "$CB_OPEN_COUNT" ]; then
    CB_OPEN_COUNT=0
fi
if [ "$CB_OPEN_COUNT" -gt 0 ]; then
    echo "   ✅ Circuit Breaker opened correctly"
else
    echo "   ❌ Circuit Breaker did NOT open"
fi

# Test call 4 (should be blocked)
echo "   Making 4th call (should be blocked immediately)..."
EMAIL="cbtest4-$(date +%s)@example.com"
START_TIME=$(date +%s%N)
curl -s -X POST "$GATEWAY_URL/api/users" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"CB Test 4\", \"email\": \"$EMAIL\"}" > /dev/null
END_TIME=$(date +%s%N)
DURATION=$(( (END_TIME - START_TIME) / 1000000 ))

if [ "$DURATION" -lt "100" ]; then
    echo "   ✅ Call 4 was blocked immediately (${DURATION}ms < 100ms)"
else
    echo "   ⚠️  Call 4 took ${DURATION}ms (expected < 100ms when CB is OPEN)"
fi

CB_BLOCKED_COUNT=$(docker compose logs "$SERVICE_NAME" --tail=300 | grep "Circuit breaker is OPEN - Call not permitted" | wc -l | tr -d ' ')
if [ -z "$CB_BLOCKED_COUNT" ]; then
    CB_BLOCKED_COUNT=0
fi
if [ "$CB_BLOCKED_COUNT" -gt 0 ]; then
    echo "   ✅ Circuit Breaker is blocking calls correctly"
else
    echo "   ❌ Circuit Breaker is NOT blocking calls"
fi
echo ""

# Restart notification service
docker compose start notification-mock > /dev/null 2>&1
sleep 2

echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Retry: $([ "$RETRY_COUNT" -gt 0 ] && echo "✅ Working" || echo "❌ Not working")"
echo "Circuit Breaker Open: $([ "$CB_OPEN_COUNT" -gt 0 ] && echo "✅ Working" || echo "❌ Not working")"
echo "Circuit Breaker Block: $([ "$CB_BLOCKED_COUNT" -gt 0 ] && echo "✅ Working" || echo "❌ Not working")"
echo ""

