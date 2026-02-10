# ── Build stage ────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# 1. Gradle wrapper (rarely changes)
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# 2. Build config files (changes when dependencies change)
COPY build.gradle settings.gradle gradle.properties ./
COPY common/build.gradle common/build.gradle
COPY modules/module-auth/build.gradle modules/module-auth/build.gradle
COPY modules/module-user/build.gradle modules/module-user/build.gradle
COPY modules/module-engagements/build.gradle modules/module-engagements/build.gradle
COPY modules/module-council/build.gradle modules/module-council/build.gradle
COPY modules/module-document/build.gradle modules/module-document/build.gradle
COPY modules/module-notification/build.gradle modules/module-notification/build.gradle
COPY modules/module-audit/build.gradle modules/module-audit/build.gradle
COPY app/build.gradle app/build.gradle

# 3. Download deps (cached unless build files change)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon

# 4. Copy source and build
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :app:bootJar --no-daemon -x test

# ── Extract stage (Spring Boot layered JAR) ───────────────────
FROM eclipse-temurin:25-jdk-alpine AS extract
WORKDIR /app
COPY --from=build /app/app/build/libs/sc-kpi-api.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ── Runtime stage ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.title="SC-KPI API" \
      org.opencontainers.image.description="Student Council KPI — backend API" \
      org.opencontainers.image.vendor="SC KPI"

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Layered COPY: least → most frequently changing
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --quiet --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-jar", "app.jar"]
