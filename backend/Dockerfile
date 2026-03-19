FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE ${PORT:-8080}
ENV SPRING_PROFILES_ACTIVE=prod
CMD java -Xmx512m -Dserver.port=${PORT:-8080} -jar app.jar
