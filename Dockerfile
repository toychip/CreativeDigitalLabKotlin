# Build stage
FROM gradle:8.14-jdk21 AS build

WORKDIR /app
COPY . .

RUN gradle clean build -x test --no-daemon

# Runtime stage
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY --from=build /app/chat-api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
