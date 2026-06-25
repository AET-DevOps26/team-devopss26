# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
WORKDIR /app
COPY api ./api
COPY services/user-service/pom.xml ./services/user-service/
COPY services/user-service/src ./services/user-service/src
WORKDIR /app/services/user-service
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /app/services/user-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]