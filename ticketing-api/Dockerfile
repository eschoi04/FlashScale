FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/build/libs/ticketing-api-*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
    CMD curl --fail --silent --show-error http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
