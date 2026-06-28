# Stage 1: Build
FROM maven:3-amazoncorretto-21 AS build
LABEL authors="Alexander Wudy"
WORKDIR /app

# Copy and install the shared module first
COPY services/shared ./services/shared
RUN mvn clean install -f services/shared/pom.xml -DskipTests

# Copy api specs and the service files
COPY api ./api
COPY services/admin-service/pom.xml ./services/admin-service/
COPY services/admin-service/src ./services/admin-service/src

WORKDIR /app/services/admin-service
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Run
FROM amazoncorretto:21
LABEL authors="Alexander Wudy"
COPY --from=build /app/services/admin-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]