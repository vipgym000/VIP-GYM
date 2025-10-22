# Use Eclipse Temurin JDK 21 and install Maven
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Install Maven
RUN apt update && apt install -y maven

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
