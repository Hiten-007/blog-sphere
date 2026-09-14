# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -o dependency:go-offline || ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package && \
    mv target/*.jar app.jar

# --- Run stage ---
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
