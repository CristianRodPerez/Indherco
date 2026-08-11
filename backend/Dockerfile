FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/postes-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "db_endpoint=${DATABASE_URL#*://}; db_endpoint=${db_endpoint#*@}; export DB_URL=jdbc:postgresql://${db_endpoint}; exec java $JAVA_OPTS -jar app.jar"]
