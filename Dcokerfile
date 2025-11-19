FROM maven:3.8.7 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

FROM openjdk:17
WORKDIR /app
COPY --from=build /app/target/*.jar invoice.jar
EXPOSE 8089

ENTRYPOINT ["java", "-Dserver.port=8089", "-jar", "invoice.jar"]
