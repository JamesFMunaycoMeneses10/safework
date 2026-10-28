# syntax=docker/dockerfile:1
# CI executes the complete isolated test suite (including H2 integration tests) first.
# Afterwards it boots this runtime image against disposable MySQL and checks /login.
# This packaging stage does not have a database; the skipped integration tests are explicit.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/safework-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --gid 10001 safework \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin safework
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/app.jar /app/app.jar
RUN mkdir /app/legacy-uploads && chown 10001:10001 /app/legacy-uploads
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
