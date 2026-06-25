# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
WORKDIR /app
COPY api ./api
COPY services/note-service/pom.xml ./services/note-service/
COPY services/note-service/src ./services/note-service/src
WORKDIR /app/services/note-service
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /app/services/note-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]