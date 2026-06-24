# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
WORKDIR /app
COPY api ./api
COPY services/checklist-service/pom.xml ./services/checklist-service/
COPY services/checklist-service/src ./services/checklist-service/src
WORKDIR /app/services/checklist-service
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /app/services/checklist-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]