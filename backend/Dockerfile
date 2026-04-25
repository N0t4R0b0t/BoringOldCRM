# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

# Copy dependency descriptors first so this layer is cached between source changes
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN gradle dependencies --no-daemon --quiet || true

# Now copy source and build the fat JAR
COPY src/ src/
RUN gradle bootJar -x test --no-daemon --quiet

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S bocrm && adduser -S bocrm -G bocrm
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown bocrm:bocrm app.jar

USER bocrm
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
