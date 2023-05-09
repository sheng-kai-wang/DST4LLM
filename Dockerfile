FROM openjdk:17

WORKDIR /app
ENV TZ=Asia/Taipei

COPY app.jar app.jar

RUN ["sudo", "service", "rsyslog", "restart"]
RUN ["sudo", "service", "cron", "restart"]

ENTRYPOINT ["java", "-jar", "app.jar"]