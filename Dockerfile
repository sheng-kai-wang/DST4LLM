FROM openjdk:17
WORKDIR /app

COPY app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]