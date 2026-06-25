# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
WORKDIR /app
COPY api ./api
COPY services/calendar-service/pom.xml ./services/calendar-service/
COPY services/calendar-service/src ./services/calendar-service/src
WORKDIR /app/services/calendar-service
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /app/services/calendar-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]