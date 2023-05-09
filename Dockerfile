FROM openjdk:17

WORKDIR /app
ENV TZ=Asia/Taipei

COPY app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]