# API Reference - Complete Guide

Complete API reference with all endpoints, examples, and test scenarios.

## Table of Contents

- [Service URLs](#service-urls)
- [User Service API](#user-service-api)
- [Health Checks](#health-checks)
- [Notification Service](#notification-service)
- [Consul Service Discovery](#consul-service-discovery)
- [Gateway Routing](#gateway-routing)
- [Error Scenarios](#error-scenarios)
- [Monitoring](#monitoring)

---

## Service URLs

### Gateway Service
- **Base URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Gateway Routes**: http://localhost:8080/actuator/gateway/routes
- **Gateway Info**: http://localhost:8080/actuator/info

### User Service (Direct Access)
- **Base URL**: http://localhost:8081
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics

### Consul UI
- **Consul Dashboard**: http://localhost:8500
- **Services List**: http://localhost:8500/ui/dc1/services

### Notification Mock Service
- **Base URL**: http://localhost:9090
- **Notification Endpoint**: http://localhost:9090/api/notifications/send

---

## User Service API

### Create User

**Endpoint**: `POST /api/users`

**Via Gateway**:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com"
  }'
```

**Direct Access** (bypass Gateway):
```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com"
  }'
```

**Request Body**:
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "createdAt": "2025-11-24T17:00:00",
  "updatedAt": null
}
```

**Notes**:
- Automatically sends welcome notification
- Email must be unique
- Email must be valid format

---

### Get User by ID

**Endpoint**: `GET /api/users/{id}`

**Via Gateway**:
```bash
curl http://localhost:8080/api/users/1
```

**Direct Access**:
```bash
curl http://localhost:8081/users/1
```

**Response** (200 OK):
```json
{
  "id": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "createdAt": "2025-11-24T17:00:00",
  "updatedAt": null
}
```

**Error Response** (404 Not Found):
```json
{
  "timestamp": "2025-11-24T17:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "path": "/users"
}
```

---

### Get User by Email

**Endpoint**: `GET /api/users/email/{email}`

**Via Gateway**:
```bash
curl http://localhost:8080/api/users/email/john@example.com
```

**Direct Access**:
```bash
curl http://localhost:8081/users/email/john@example.com
```

**Response** (200 OK):
```json
{
  "id": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "createdAt": "2025-11-24T17:00:00",
  "updatedAt": null
}
```

---

### Get All Users

**Endpoint**: `GET /api/users`

**Via Gateway**:
```bash
curl http://localhost:8080/api/users
```

**Direct Access**:
```bash
curl http://localhost:8081/users
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "email": "john@example.com",
    "name": "John Doe",
    "createdAt": "2025-11-24T17:00:00",
    "updatedAt": null
  },
  {
    "id": 2,
    "email": "jane@example.com",
    "name": "Jane Smith",
    "createdAt": "2025-11-24T17:01:00",
    "updatedAt": null
  }
]
```

---

## Health Checks

### Gateway Health

```bash
curl http://localhost:8080/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "consul": {
      "status": "UP",
      "details": {
        "services": ["consul", "gateway-service", "user-service"]
      }
    }
  }
}
```

### User Service Health

```bash
curl http://localhost:8081/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2"
      }
    },
    "consul": {
      "status": "UP"
    }
  }
}
```

### Gateway Routes

```bash
curl http://localhost:8080/actuator/gateway/routes
```

Shows all configured routes including the route to `user-service` via Consul.

---

## Notification Service

### Test Notification Mock Directly

```bash
curl -X POST http://localhost:9090/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "message": "Test notification"
  }'
```

**Response**:
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SENT",
  "message": "Notification sent successfully",
  "sentAt": "2025-11-24T17:00:00"
}
```

### Verify Notification When Creating User

When you create a user, the system automatically sends a welcome notification.

**Check logs for notification status**:

```bash
# View recent notification logs
docker compose logs user-service --tail=30 | grep -E "✅|⚠️|Notification"

# Monitor in real-time
docker compose logs -f user-service | grep --line-buffered -E "✅|⚠️|Notification"
```

**Success log** (✅):
```
INFO ... UserService : Sending welcome notification for user: john@example.com
INFO ... UserService : Notification request: recipient=john@example.com, message=Welcome John Doe! Your account has been created.
INFO ... UserService : ✅ Notification sent successfully: status = SENT, notificationId = ..., sentAt = ...
```

**Fallback log** (⚠️):
```
INFO ... UserService : Sending welcome notification for user: john@example.com
WARN ... UserService : ⚠️ Notification service unavailable, using fallback. Error: ...
WARN ... UserService : Fallback: User will be created without sending notification
INFO ... UserService : User created without notification: john@example.com
```

### Test Circuit Breaker

1. Stop notification mock:
   ```bash
   docker compose stop notification-mock
   ```

2. Create a user (should still work with fallback):
   ```bash
   curl -X POST http://localhost:8080/api/users \
     -H "Content-Type: application/json" \
     -d '{"name": "Test User", "email": "test@example.com"}'
   ```

3. Check logs for fallback:
   ```bash
   docker compose logs user-service --tail=10 | grep -i fallback
   ```

4. Restart notification mock:
   ```bash
   docker compose start notification-mock
   ```

---

## Consul Service Discovery

### View Services in Consul UI

1. Open browser: http://localhost:8500
2. Click on **Services** tab
3. You should see:
   - `gateway-service` (1 instance, healthy)
   - `user-service` (1 instance, healthy)

### Check Service Details via Consul API

```bash
# List all services
curl http://localhost:8500/v1/agent/services

# Get user-service details
curl http://localhost:8500/v1/health/service/user-service

# Get gateway-service details
curl http://localhost:8500/v1/health/service/gateway-service
```

---

## Gateway Routing

### How Gateway Routes Work

When you call `http://localhost:8080/api/users/1`:

1. Request hits Gateway on port 8080
2. Gateway queries Consul: "Where is user-service?"
3. Consul returns: "user-service is at http://user-service:8081"
4. Gateway forwards request to User Service (removes `/api` prefix)
5. User Service processes and returns response
6. Gateway returns response to client

### Verify Gateway is Using Service Discovery

```bash
# Check Gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Check Gateway logs
docker compose logs gateway-service | grep -i consul
```

Should show route with `uri: lb://user-service` (load-balanced via Consul).

---

## Error Scenarios

### Validation Errors

**Invalid Email**:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "invalid-email"}'
```

**Response** (400 Bad Request):
```json
{
  "timestamp": "2025-11-24T17:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "validationErrors": {
    "email": "Invalid email format"
  }
}
```

**Missing Required Fields**:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User"}'
```

**Response** (400 Bad Request): Email is required

**Duplicate Email**:
```bash
# Create first user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "First", "email": "duplicate@example.com"}'

# Try to create second user with same email
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Second", "email": "duplicate@example.com"}'
```

**Response** (400 Bad Request):
```json
{
  "timestamp": "2025-11-24T17:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "User with email duplicate@example.com already exists"
}
```

### Not Found Errors

**User Not Found**:
```bash
curl http://localhost:8080/api/users/999
```

**Response** (404 Not Found):
```json
{
  "timestamp": "2025-11-24T17:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "path": "/users"
}
```

---

## Monitoring

### View Service Logs

```bash
# Gateway logs
docker compose logs -f gateway-service

# User Service logs
docker compose logs -f user-service

# User Service logs (filter for notifications)
docker compose logs -f user-service | grep --line-buffered -E "✅|⚠️|Notification"

# Notification Mock Service logs
docker compose logs -f notification-mock-service

# All logs
docker compose logs -f
```

### Check Metrics

```bash
# Gateway metrics
curl http://localhost:8080/actuator/metrics

# User Service metrics
curl http://localhost:8081/actuator/metrics
```

---

## Quick Reference Table

| Action | Method | URL | Description |
|--------|--------|-----|-------------|
| Create User | POST | http://localhost:8080/api/users | Create new user |
| Get User | GET | http://localhost:8080/api/users/{id} | Get user by ID |
| Get User by Email | GET | http://localhost:8080/api/users/email/{email} | Get user by email |
| Get All Users | GET | http://localhost:8080/api/users | List all users |
| Gateway Health | GET | http://localhost:8080/actuator/health | Gateway health check |
| User Service Health | GET | http://localhost:8081/actuator/health | User service health check |
| Gateway Routes | GET | http://localhost:8080/actuator/gateway/routes | View Gateway routes |
| Consul UI | GET | http://localhost:8500 | Consul dashboard |

---

## Tips

1. **Pretty print JSON responses**:
   ```bash
   curl http://localhost:8080/api/users/1 | jq .
   ```

2. **Verbose output**:
   ```bash
   curl -v http://localhost:8080/api/users/1
   ```

3. **Save responses to file**:
   ```bash
   curl http://localhost:8080/api/users/1 > response.json
   ```

4. **Use Postman or Insomnia** for GUI testing:
   - Import the URLs above
   - Create a collection with all endpoints

