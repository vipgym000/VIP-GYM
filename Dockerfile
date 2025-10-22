# -------- Build Stage --------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Install Maven (you can skip this if using mvnw)
RUN apt-get update && apt-get install -y maven

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy only the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your app uses
EXPOSE 8443

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
