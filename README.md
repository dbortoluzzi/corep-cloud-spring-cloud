# Spring Boot 3 - Spring Cloud Demo

A minimal Spring Boot 3 application demonstrating Spring Cloud capabilities for building cloud-ready microservices.

## Use Case

This demo implements a simple **User Management System** that demonstrates how Spring Cloud enables cloud-ready microservices:

### Scenario

A **User Service** that:
1. **Manages users** - Create, retrieve users (name, email)
2. **Sends welcome notifications** - When a new user is created, automatically sends a welcome email via an external Notification Service
3. **Handles failures gracefully** - If the Notification Service is down, the user is still created (degraded mode)

### Architecture Flow

```
Client Application
    ↓
    POST /api/users {"name": "John", "email": "john@example.com"}
    ↓
Gateway Service (port 8080)
    ↓ [Queries Consul: "Where is user-service?"]
    ↓ [Consul: "user-service is at http://user-service:8081"]
    ↓ Routes to: POST /users
    ↓
User Service (port 8081)
    ↓ [Business Logic: Save user to database]
    ↓ [Calls Notification Service via OpenFeign]
    ↓
Notification Service Mock (port 9090)
    ↓ [Sends welcome email]
    ↓
Response: User created + Notification sent
```

### Key Spring Cloud Features Demonstrated

1. **Service Discovery (Consul)**: 
   - Gateway automatically discovers User Service location
   - No hardcoded URLs - services find each other dynamically
   - **See it**: Open http://localhost:8500 → Services tab

2. **Spring Cloud Gateway**: 
   - Single entry point for all clients
   - Dynamic routing based on service discovery
   - Load balancing (when multiple instances exist)

3. **OpenFeign**: 
   - Declarative REST client (no boilerplate HTTP code)
   - User Service calls Notification Service with simple interface

4. **Resilience4j**: 
   - Circuit Breaker: Prevents calls to down services
   - Retry: Automatically retries on transient errors
   - Fallback: Creates user even if notification fails

### Real-World Application

This pattern is used in production systems where:
- Services need to communicate with each other
- Services can be unstable or have network issues
- You want graceful degradation (system works even if some services fail)
- Services are deployed in cloud/Kubernetes (dynamic IPs/ports)

## Architecture

```
Client
  ↓
Gateway Service (port 8080) - Single entry point
  ↓ [Queries Consul: "Where is user-service?"]
  ↓ [Consul: "user-service is at http://user-service:8081"]
  ↓
User Service (port 8081) - Business logic
  ↓ [Calls via OpenFeign]
  ↓
Notification Service Mock (port 9090) - External service
```

## Components

- **Gateway Service**: API Gateway with dynamic routing via Consul
- **User Service**: Manages users, calls Notification Service via OpenFeign
- **Consul**: Service registry and discovery
- **Notification Mock**: Mock service for testing

## Spring Cloud Features Demonstrated

1. **Service Discovery (Consul)**: Services auto-register and discover each other
2. **Spring Cloud Gateway**: Dynamic routing based on service discovery
3. **OpenFeign**: Declarative REST clients
4. **Resilience4j**: Circuit breaker, retry, timeout patterns

## Prerequisites

Before starting, ensure you have:

- **Java 17+** installed
  ```bash
  java -version
  ```

- **Maven 3.6+** installed
  ```bash
  mvn -version
  ```

- **Docker** and **Docker Compose** installed
  ```bash
  docker --version
  docker compose version
  ```

## Building the Project

### Build All Modules

From the project root directory:

```bash
mvn clean package
```

This will:
- Compile all Java code
- Run tests (if any)
- Package both `gateway-service` and `user-service` as JAR files
- Create JARs in `target/` directories of each module

### Build Individual Modules

```bash
# Build only Gateway Service
cd gateway-service
mvn clean package

# Build only User Service
cd user-service
mvn clean package
```

### Build Output

After successful build, you should see:
- `gateway-service/target/gateway-service-1.0.0.jar`
- `user-service/target/user-service-1.0.0.jar`

## Running the Application

### Option 1: Docker Compose (Recommended)

This is the easiest way to run everything:

```bash
# Start all services (Consul, Gateway, User Service, Mock)
docker compose up -d

# View logs from all services
docker compose logs -f

# View logs from specific service
docker compose logs -f gateway-service
docker compose logs -f user-service

# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v
```

### Option 2: Run Locally (Without Docker)

#### Step 1: Start Consul

```bash
# Using Docker
docker run -d --name consul -p 8500:8500 hashicorp/consul:1.17 agent -server -ui -bootstrap-expect=1 -client=0.0.0.0

# Or download and run Consul binary
# https://www.consul.io/downloads
```

#### Step 2: Start Notification Mock Service

```bash
docker run -d --name notification-mock -p 9090:1080 \
  -v $(pwd)/mockserver-config.json:/config/mockserver-init.json \
  -e MOCKSERVER_INITIALIZATION_JSON_PATH=/config/mockserver-init.json \
  mockserver/mockserver:5.15.0
```

#### Step 3: Start User Service

```bash
cd user-service
mvn spring-boot:run
```

User Service will start on port **8081** and register itself in Consul.

#### Step 4: Start Gateway Service

In a new terminal:

```bash
cd gateway-service
mvn spring-boot:run
```

Gateway Service will start on port **8080** and register itself in Consul.

## Verifying Services are Running

### Check Docker Containers

```bash
docker compose ps
```

You should see:
- `consul-server` - Running
- `gateway-service` - Running
- `user-service` - Running
- `notification-mock-service` - Running

### Check Service Health

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# User Service health
curl http://localhost:8081/actuator/health
```

### Verify Consul Service Discovery

1. **Open Consul UI**: http://localhost:8500

2. **Check Services Tab**:
   - You should see `gateway-service` and `user-service` listed
   - Both should show as "healthy"

3. **Click on a service** to see:
   - IP address and port
   - Health check status
   - Tags and metadata

4. **Check Service Registration Logs**:
   ```bash
   docker compose logs user-service | grep -i consul
   docker compose logs gateway-service | grep -i consul
   ```

   You should see messages like:
   ```
   Registering service with consul: user-service
   Service registered successfully
   ```

### Verify Gateway Routing

```bash
# Check Gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Should show route to user-service discovered from Consul
```

## Project Structure

```
core-cloud-spring-cloud/
├── pom.xml (parent POM)
├── gateway-service/
│   ├── pom.xml
│   ├── src/main/java/com/example/gateway/
│   │   └── GatewayApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-docker.yml
│   └── Dockerfile
├── user-service/
│   ├── pom.xml
│   ├── src/main/java/com/example/userservice/
│   │   ├── UserServiceApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── client/
│   │   ├── config/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── dto/
│   │   └── exception/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-docker.yml
│   └── Dockerfile
├── docker-compose.yml
├── mockserver-config.json
├── README.md (this file)
└── test/
    ├── TESTING.md (test URLs and examples)
    ├── README.md (testing guide)
    └── DEMO_GUIDE.md (presentation guide)
```

## Configuration Files

### Gateway Service Configuration

- **Local**: `gateway-service/src/main/resources/application.yml`
- **Docker**: `gateway-service/src/main/resources/application-docker.yml`

Key settings:
- Port: 8080
- Consul host: localhost (local) / consul (docker)
- Routes: `/api/users/**` → `lb://user-service`

### User Service Configuration

- **Local**: `user-service/src/main/resources/application.yml`
- **Docker**: `user-service/src/main/resources/application-docker.yml`

Key settings:
- Port: 8081
- Consul host: localhost (local) / consul (docker)
- H2 database: in-memory
- Notification service URL: http://localhost:9090 (local) / http://notification-mock:1080 (docker)

## Troubleshooting

### Services not appearing in Consul UI

1. Check if Consul is running:
   ```bash
   docker compose ps consul
   ```

2. Check service logs:
   ```bash
   docker compose logs user-service | grep -i consul
   docker compose logs gateway-service | grep -i consul
   ```

3. Verify network connectivity:
   ```bash
   docker compose exec user-service ping consul
   ```

### Gateway not routing to User Service

1. Verify User Service is registered in Consul UI (http://localhost:8500)

2. Check Gateway logs:
   ```bash
   docker compose logs gateway-service
   ```

3. Verify route configuration:
   ```bash
   cat gateway-service/src/main/resources/application.yml
   ```

4. Check Gateway routes:
   ```bash
   curl http://localhost:8080/actuator/gateway/routes
   ```

### Notification service not responding

1. Check if mock service is running:
   ```bash
   docker compose ps notification-mock-service
   ```

2. Test mock service directly:
   ```bash
   curl -X POST http://localhost:9090/api/notifications/send \
     -H "Content-Type: application/json" \
     -d '{"recipient": "test@example.com", "message": "Test"}'
   ```

3. Check User Service logs for Feign errors:
   ```bash
   docker compose logs user-service | grep -i feign
   ```

### Build Errors

1. **Java version mismatch**:
   - Ensure Java 17+ is installed
   - Check: `java -version`

2. **Maven dependencies not downloading**:
   - Check internet connection
   - Try: `mvn clean install -U`

3. **Docker build fails**:
   - Ensure Docker is running
   - Check: `docker ps`

### Port Already in Use

If you get "port already in use" errors:

```bash
# Find process using port 8080
lsof -i :8080

# Find process using port 8081
lsof -i :8081

# Kill the process (replace PID with actual process ID)
kill -9 <PID>
```

## Stopping Services

### Stop Docker Compose

```bash
# Stop all services
docker compose down

# Stop and remove volumes (clears data)
docker compose down -v

# Stop and remove images
docker compose down --rmi all
```

### Stop Local Services

Press `Ctrl+C` in the terminal where the service is running.

## Documentation

- **[HOW_IT_WORKS.md](HOW_IT_WORKS.md)** - **How Consul Service Discovery and Spring Cloud Gateway work** (detailed explanation)
- **[test/TESTING.md](test/TESTING.md)** - Quick testing guide with essential commands
- **[test/README.md](test/README.md)** - Detailed testing guide and scripts
- **[test/DEMO_GUIDE.md](test/DEMO_GUIDE.md)** - Step-by-step demo presentation guide
- **[API_REFERENCE.md](API_REFERENCE.md)** - Complete API reference with all endpoints and examples

## Next Steps

- Add authentication/authorization to Gateway
- Implement rate limiting
- Add distributed tracing (Zipkin, Jaeger)
- Scale services: `docker compose up -d --scale user-service=3`
- Add monitoring (Prometheus, Grafana)
