FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY ./build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]