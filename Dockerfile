FROM openjdk:17

WORKDIR /app
ENV TZ=Asia/Taipei

COPY app.jar app.jar

RUN ["service", "rsyslog", "restart"]
RUN ["service", "cron", "restart"]

ENTRYPOINT ["java", "-jar", "app.jar"]d