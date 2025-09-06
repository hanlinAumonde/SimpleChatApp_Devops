FROM openjdk:17-jdk-alpine

WORKDIR /app

VOLUME /tmp

COPY target/message-service-1.0.0.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","app.jar"]