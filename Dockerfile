# syntax=docker/dockerfile:1

FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk AS backend
WORKDIR /backend
COPY --chmod=755 backend/gradlew ./
COPY backend/gradle gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/src src
# the jar serves whatever sits in static/
COPY --from=frontend /frontend/dist src/main/resources/static
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon && \
    mv build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 kinetiq
USER kinetiq
WORKDIR /application
COPY --from=backend /backend/extracted/dependencies/ ./
COPY --from=backend /backend/extracted/spring-boot-loader/ ./
COPY --from=backend /backend/extracted/snapshot-dependencies/ ./
COPY --from=backend /backend/extracted/application/ ./
EXPOSE 8080
# the JVM default is 25% of container memory, too little for Spring and Hibernate
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
