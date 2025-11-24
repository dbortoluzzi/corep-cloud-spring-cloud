#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
SERVICE_NAME="user-service"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Resilience4j Test Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to show filtered logs
show_filtered_logs() {
    local pattern="$1"
    local tail_lines="${2:-100}"
    echo -e "${YELLOW}=== Filtered Logs (pattern: $pattern) ===${NC}"
    docker compose logs "$SERVICE_NAME" --tail="$tail_lines" | grep -E "$pattern" | tail -20
    echo ""
}

# Function to test Retry
test_retry() {
    echo -e "${GREEN}=== TEST 1: Retry (3 Attempts) ===${NC}"
    echo ""
    
    # Stop notification service
    echo "Stopping notification service..."
    docker compose stop notification-mock > /dev/null 2>&1
    sleep 2
    
    # Create user (will trigger retries)
    echo "Creating user (will trigger retries)..."
    EMAIL="retrytest-$(date +%s)@example.com"
    curl -s -X POST "$GATEWAY_URL/api/users" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"Retry Test\", \"email\": \"$EMAIL\"}" > /dev/null
    
    echo "Waiting for retries to complete (5 seconds)..."
    sleep 5
    
    # Show retry logs
    show_filtered_logs "RETRY|📧 Calling notification|🔄|❌|retrytest|✅ RETRY SUCCESS" 200
    
    echo -e "${GREEN}Expected:${NC}"
    echo "  - 📧 Calling notification service (appears 3 times)"
    echo "  - 🔄 RETRY #1, 🔄 RETRY #2"
    echo "  - ❌ RETRY FAILED"
    echo ""
    
    # Restart notification service
    docker compose start notification-mock > /dev/null 2>&1
    sleep 2
}

# Function to test Circuit Breaker
test_circuit_breaker() {
    echo -e "${GREEN}=== TEST 2: Circuit Breaker (Opens after 3 failures) ===${NC}"
    echo ""
    
    # Stop notification service
    echo "Stopping notification service..."
    docker compose stop notification-mock > /dev/null 2>&1
    sleep 2
    
    # Make 3 calls to open Circuit Breaker
    echo "Making 3 calls to open Circuit Breaker..."
    for i in {1..3}; do
        EMAIL="cbtest$i-$(date +%s)@example.com"
        echo "  Call $i: Creating user $EMAIL"
        curl -s -X POST "$GATEWAY_URL/api/users" \
            -H "Content-Type: application/json" \
            -d "{\"name\": \"CB Test $i\", \"email\": \"$EMAIL\"}" > /dev/null
        sleep 2
    done
    
    echo "Waiting for Circuit Breaker to open (3 seconds)..."
    sleep 3
    
    # Show Circuit Breaker state transition
    echo ""
    show_filtered_logs "Circuit breaker state transition|OPEN|CLOSED|HALF_OPEN" 500
    
    # Make 4th call (should be blocked immediately)
    echo -e "${YELLOW}Making 4th call (should be BLOCKED immediately, NO retry)...${NC}"
    EMAIL="cbtest4-$(date +%s)@example.com"
    START_TIME=$(date +%s%N)
    curl -s -X POST "$GATEWAY_URL/api/users" \
        -H "Content-Type: application/json" \
        -d "{\"name\": \"CB Test 4\", \"email\": \"$EMAIL\"}" > /dev/null
    END_TIME=$(date +%s%N)
    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    
    echo -e "${GREEN}Call 4 duration: ${DURATION}ms (should be < 100ms if CB is OPEN)${NC}"
    echo ""
    
    sleep 2
    
    # Show logs for call 4 (should have NO retry)
    echo -e "${YELLOW}=== Logs for Call 4 (should have NO retry when CB is OPEN) ===${NC}"
    docker compose logs "$SERVICE_NAME" --tail=500 | grep -E "$EMAIL|Call not permitted|🚫|Circuit breaker is OPEN" | head -10
    echo ""
    
    echo -e "${GREEN}Expected:${NC}"
    echo "  - 🔌 Circuit breaker state transition: CLOSED -> OPEN (after 3 calls)"
    echo "  - 🚫 Circuit breaker is OPEN - Call not permitted (for call 4)"
    echo "  - NO retry logs for call 4"
    echo "  - Call 4 duration < 100ms"
    echo ""
}

# Function to monitor logs in real-time
monitor_logs() {
    echo -e "${GREEN}=== Real-time Log Monitoring ===${NC}"
    echo "Monitoring logs for: RETRY, Circuit breaker, 📧, 🔄, ❌, 🚫, 🔌"
    echo "Press Ctrl+C to stop"
    echo ""
    docker compose logs -f "$SERVICE_NAME" | grep --line-buffered -E "RETRY|Circuit breaker|📧|🔄|❌|🚫|🔌|Call not permitted"
}

# Main menu
case "$1" in
    retry)
        test_retry
        ;;
    circuit-breaker|cb)
        test_circuit_breaker
        ;;
    monitor|watch)
        monitor_logs
        ;;
    all)
        test_retry
        echo ""
        echo -e "${BLUE}========================================${NC}"
        echo ""
        test_circuit_breaker
        ;;
    *)
        echo "Usage: $0 {retry|circuit-breaker|cb|monitor|watch|all}"
        echo ""
        echo "Commands:"
        echo "  retry           - Test Retry mechanism (3 attempts)"
        echo "  circuit-breaker - Test Circuit Breaker (opens after 3 failures)"
        echo "  cb              - Alias for circuit-breaker"
        echo "  monitor         - Monitor logs in real-time"
        echo "  watch           - Alias for monitor"
        echo "  all             - Run both tests"
        echo ""
        echo "Examples:"
        echo "  $0 retry              # Test Retry"
        echo "  $0 circuit-breaker    # Test Circuit Breaker"
        echo "  $0 monitor            # Monitor logs in real-time"
        echo "  $0 all                # Run all tests"
        exit 1
        ;;
esac

