FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/user-service-1.0.0.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]

