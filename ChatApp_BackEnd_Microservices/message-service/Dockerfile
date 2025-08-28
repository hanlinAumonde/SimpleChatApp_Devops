FROM openjdk:17-jdk-slim

VOLUME /tmp

COPY target/message-service-1.0.0.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]