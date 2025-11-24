# How to Disable Retry or Circuit Breaker

To disable Retry or Circuit Breaker, you must **comment out the annotations** in the code.

## Disable Retry

Comment out the `@Retry` annotation in `NotificationService.java`:

1. Open `user-service/src/main/java/com/example/userservice/service/NotificationService.java`
2. Comment out the `@Retry` annotation:

```java
@CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackNotification")
// @Retry(name = "notification-service")  // Disabled
public NotificationResponse sendNotification(NotificationRequest request) {
    log.info("📧 Calling notification service");
    return notificationClient.sendNotification(request);
}
```

3. Rebuild and restart:
```bash
mvn clean package -DskipTests -pl user-service
docker compose build user-service
docker compose up -d user-service
```

## Disable Circuit Breaker

Comment out the `@CircuitBreaker` annotation in `NotificationService.java`:

```java
// @CircuitBreaker(name = "notification-service", fallbackMethod = "fallbackNotification")  // Disabled
@Retry(name = "notification-service")
public NotificationResponse sendNotification(NotificationRequest request) {
    log.info("📧 Calling notification service");
    return notificationClient.sendNotification(request);
}
```

**Note:** If you disable Circuit Breaker, the `fallbackNotification` method will not be called automatically. You may want to handle errors differently.

3. Rebuild and restart:
```bash
mvn clean package -DskipTests -pl user-service
docker compose build user-service
docker compose up -d user-service
```

## Verification

After disabling, test to verify:

### Test Retry Disabled

```bash
cd test
./test-resilience.sh retry
```

**Expected:** No retry logs (only initial call, no `🔄 RETRY #1`, `🔄 RETRY #2`)

### Test Circuit Breaker Disabled

```bash
cd test
./test-resilience.sh circuit-breaker
```

**Expected:** No Circuit Breaker state transitions (no `🔌 Circuit breaker state transition`)

## Alternative: Configure YAML to Never Activate

Instead of commenting out annotations, you can modify the YAML configuration to effectively disable the features:

### Disable Retry via YAML

Set `max-attempts: 1` in `application.yml`:

```yaml
resilience4j:
  retry:
    instances:
      notification-service:
        max-attempts: 1  # Only initial attempt, no retries
```

This way, Retry will only make 1 attempt (the initial call), effectively disabling retry behavior without removing the annotation.

**Advantage:** No code changes needed, just restart the service.

### Disable Circuit Breaker via YAML

Configure Circuit Breaker with very high thresholds so it never opens:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      notification-service:
        failure-rate-threshold: 100  # Never opens (100% failure rate needed)
        minimum-number-of-calls: 10000  # Very high threshold
        sliding-window-size: 10000
```

This makes it extremely unlikely for the Circuit Breaker to open, effectively disabling it.

**Advantage:** No code changes needed, just restart the service.

**Note:** The Circuit Breaker will still be active (monitoring calls), but it will never transition to OPEN state under normal conditions.

## Comparison

| Method | Retry | Circuit Breaker | Requires Rebuild | Code Changes |
|--------|-------|----------------|------------------|--------------|
| Comment annotation | ✅ | ✅ | Yes | Yes |
| YAML configuration | ✅ | ✅ | No | No |

**Recommendation:**
- **For permanent disable:** Comment out annotations (clearer intent)
- **For temporary disable or testing:** Use YAML configuration (faster, no rebuild)

