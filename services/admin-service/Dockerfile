# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]