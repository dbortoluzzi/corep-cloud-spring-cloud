#!/bin/bash

# Simple script to show filtered logs for Resilience4j
# Usage: ./test-resilience-simple.sh [pattern] [tail-lines]

SERVICE_NAME="user-service"
PATTERN="${1:-RETRY|Circuit breaker|📧|🔄|❌|🚫|🔌|Call not permitted}"
TAIL_LINES="${2:-200}"

echo "=== Filtered Logs (pattern: $PATTERN, last $TAIL_LINES lines) ==="
echo ""
docker compose logs "$SERVICE_NAME" --tail="$TAIL_LINES" | grep -E "$PATTERN"
echo ""

