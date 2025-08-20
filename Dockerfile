FROM openjdk:21-jdk-slim AS build
WORKDIR /app
COPY target/device-data-hub-teleofis*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]