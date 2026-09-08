FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN addgroup --system yardflow && adduser --system --ingroup yardflow yardflow
COPY --from=build /app/target/operation-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R yardflow:yardflow /app
USER yardflow
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
